package org.zotero.android.sync

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.zotero.android.BuildConfig
import org.zotero.android.api.SyncApi
import org.zotero.android.api.mappers.CollectionResponseMapper
import org.zotero.android.api.mappers.ItemResponseMapper
import org.zotero.android.api.mappers.SearchResponseMapper
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.requests.StoreCollectionsDbRequest
import org.zotero.android.database.requests.StoreItemsDbResponseRequest
import org.zotero.android.database.requests.StoreSearchesDbRequest
import org.zotero.android.files.FileStore
import timber.log.Timber
import java.io.FileWriter
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

typealias SyncBatchResponse = Triple<List<String>, List<Throwable>, List<StoreItemsResponse.Error>>

class SyncBatchProcessor(
    val batches: List<DownloadBatch>,
    val userId: Long,
    val syncApi: SyncApi,
    val dbWrapper: DbWrapper,
    val fileStore: FileStore,
    val itemResponseMapper: ItemResponseMapper,
    val collectionResponseMapper: CollectionResponseMapper,
    val searchResponseMapper: SearchResponseMapper,
    val schemaController: SchemaController,
    val dateParser: DateParser,
    val gson: Gson,
    val progress: (Int) -> Unit,
    val completion: suspend (CustomResult<SyncBatchResponse>) -> Unit,
) {

    private var failedIds = Collections.synchronizedList(mutableListOf<String>())
    private var parsingErrors = Collections.synchronizedList(mutableListOf<Throwable>())
    private var itemConflicts= Collections.synchronizedList(mutableListOf<StoreItemsResponse.Error>())
    private var isFinished: AtomicBoolean = AtomicBoolean(false)
    private var processedCount: AtomicInteger = AtomicInteger(0)

    private val limitedParallelismDispatcher = Dispatchers.IO.limitedParallelism(4)
    private val resultsProcessorCoroutineScope = CoroutineScope(limitedParallelismDispatcher)

    suspend fun start() {
        this.batches.map { batch ->
            val keysString = batch.keys.joinToString(separator = ",")
            val url =
                BuildConfig.BASE_API_URL + "/" + batch.libraryId.apiPath(userId = this.userId) + "/" + batch.objectS.apiPath

            resultsProcessorCoroutineScope.launch {
                val networkResult = safeApiCall {
                    val parameters = mutableMapOf<String, String>()
                    when (batch.objectS) {
                        SyncObject.collection ->
                            parameters["collectionKey"] = keysString

                        SyncObject.item, SyncObject.trash ->
                            parameters["itemKey"] = keysString

                        SyncObject.search ->
                            parameters["searchKey"] = keysString

                        SyncObject.settings -> {}
                    }
                    syncApi.objects(url = url, queryMap = parameters)
                }
                if (!isActive) {
                    return@launch
                }
                process(result = networkResult, batch = batch)
            }
        }
    }

    private suspend fun process(result: CustomResult<JsonArray>, batch: DownloadBatch) {
        if (isFinished.get()) {
            return
        }

        when (result) {
            is CustomResult.GeneralSuccess.NetworkSuccess -> {
                val lastModifiedVersion = result.lastModifiedVersion
                val payload = result.value!!
                process(data = payload, lastModifiedVersion = lastModifiedVersion, batch = batch)
            }
            is CustomResult.GeneralError -> {
                cancel(result)
            }
            else -> {}
        }
    }

    private suspend fun process(data: JsonArray, lastModifiedVersion: Int, batch: DownloadBatch) {
        if (this.isFinished.get()) {
            return
        }

        if (batch.version != lastModifiedVersion) {
            cancel(CustomResult.GeneralError.CodeError(SyncError.NonFatal.versionMismatch(batch.libraryId)))
            return
        }
        try {
            val response = sync(
                dataArray = data,
                libraryId = batch.libraryId,
                objectS = batch.objectS,
                expectedKeys = batch.keys
            )
            this.progress(batch.keys.size)
            finish(response)

        } catch (e: Exception) {
            cancel(CustomResult.GeneralError.CodeError(e))
        }

    }

    private suspend fun finish(response: Triple<List<String>, List<Throwable>, List<StoreItemsResponse.Error>>) {
        if (this.isFinished.get()) {
            return
        }
        response.first.forEach {
            this.failedIds.add(it)
        }

        response.second.forEach {
            this.parsingErrors.add(it)
        }

        response.third.forEach {
            this.itemConflicts.add(it)
        }
        this.processedCount.incrementAndGet()

        if (this.processedCount.get() == this.batches.size) {
            completion(CustomResult.GeneralSuccess(SyncBatchResponse(this.failedIds, this.parsingErrors, this.itemConflicts)))
            this.isFinished.set(true)
        }

    }

    private fun sync(
        dataArray: JsonArray,
        libraryId: LibraryIdentifier,
        objectS: SyncObject,
        expectedKeys: List<String>
    ): SyncBatchResponse {
        return when (objectS) {
            SyncObject.collection -> {
                val objects = mutableListOf<JsonElement>()
                val errors = mutableListOf<Throwable>()
                val collections = dataArray.mapNotNull {
                    try {
                        objects.add(it)
                        collectionResponseMapper.fromJson(it.asJsonObject)
                    } catch (e :Exception) {
                        Timber.e(e)
                        errors.add(e)
                        null
                    }
                }

                storeIndividualObjects(objects, type = SyncObject.collection, libraryId = libraryId)
                dbWrapper.realmDbStorage.perform(request = StoreCollectionsDbRequest(response = collections))

                val failedKeys =
                    failedKeys(expectedKeys = expectedKeys, parsedKeys = collections.map { it.key })

                SyncBatchResponse(failedKeys, errors, emptyList())
            }
            SyncObject.search -> {
                val objects = mutableListOf<JsonElement>()
                val errors = mutableListOf<Throwable>()

                val searches = dataArray.mapNotNull {
                    try {
                        objects.add(it)
                        searchResponseMapper.fromJson(it.asJsonObject)
                    } catch (e :Exception) {
                        Timber.e(e)
                        errors.add(e)
                        null
                    }
                }
                storeIndividualObjects(objects, SyncObject.search, libraryId = libraryId)

                dbWrapper.realmDbStorage.perform(request = StoreSearchesDbRequest(response = searches))

                val failedKeys =
                    failedKeys(expectedKeys = expectedKeys, parsedKeys = searches.map { it.key })

                SyncBatchResponse(failedKeys, errors, emptyList())
            }
            SyncObject.item, SyncObject.trash -> {
                val objects = mutableListOf<JsonElement>()
                val errors = mutableListOf<Throwable>()
                val items = dataArray.mapNotNull {
                    try {
                        objects.add(it)
                        itemResponseMapper.fromJson(it.asJsonObject, schemaController)
                    } catch (e :Exception) {
                        Timber.e(e)
                        errors.add(e)
                        null
                    }
                }
                //Set a breakpoint here

                storeIndividualObjects(objects, type = SyncObject.item, libraryId = libraryId)

                val request = StoreItemsDbResponseRequest(
                    responses = items,
                    schemaController = this.schemaController,
                    dateParser = this.dateParser,
                    preferResponseData = true,
                    denyIncorrectCreator = true,
                )
                val response = dbWrapper.realmDbStorage.perform(request = request, invalidateRealm = true)
                val failedKeys =
                    failedKeys(expectedKeys = expectedKeys, parsedKeys = items.map { it.key })

                renameExistingFiles(changes = response.changedFilenames, libraryId = libraryId)
                SyncBatchResponse(failedKeys, errors, response.conflicts)

            }
            SyncObject.settings -> {
                SyncBatchResponse(emptyList(), emptyList(), emptyList())
            }
        }
    }

    private fun failedKeys(expectedKeys: List<String>, parsedKeys: List<String>): List<String> {
        return expectedKeys.filter { !parsedKeys.contains(it) }
    }

    private fun renameExistingFiles(changes: List<StoreItemsResponse.FilenameChange>, libraryId: LibraryIdentifier) {
        for (change in changes) {
            val oldFile = fileStore.attachmentFile(libraryId, key = change.key, filename = change.oldName)
            if (!oldFile.exists()) {
                continue
            }

            val newFile = fileStore.attachmentFile(libraryId, key = change.key, filename = change.newName)
            if (!oldFile.renameTo(newFile)) {
                Timber.e("SyncBatchProcessor: can't rename file")
                oldFile.deleteRecursively()
            }
        }
    }

    private fun storeIndividualObjects(jsonObjects: List<JsonElement>, type: SyncObject, libraryId: LibraryIdentifier) {
        for (obj in jsonObjects) {
            val objectS = obj.asJsonObject
            val key = objectS["key"]?.asString ?: continue
            try {
                val file = fileStore.jsonCacheFile(type, libraryId = libraryId, key = key)
                val fileWriter = FileWriter(file)
                gson.toJson(objectS, fileWriter)
                fileWriter.flush()
                fileWriter.close()
                println("")
            } catch (e: Throwable) {
                Timber.e(e, "SyncBatchProcessor: can't encode/write item - $objectS")
            }
        }
    }

    private suspend fun cancel(error: CustomResult.GeneralError) {
        cancelAllOperations()
        this.isFinished.set(true)
        completion(error)
    }

    fun cancelAllOperations() {
        resultsProcessorCoroutineScope.cancel()
        limitedParallelismDispatcher.cancel()
    }

}