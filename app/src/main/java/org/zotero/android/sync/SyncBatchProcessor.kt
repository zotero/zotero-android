package org.zotero.android.sync

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import org.zotero.android.BuildConfig
import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.data.mappers.CollectionResponseMapper
import org.zotero.android.data.mappers.ItemResponseMapper
import org.zotero.android.files.FileStore
import timber.log.Timber

typealias SyncBatchResponse = Triple<List<String>, List<Throwable>, List<StoreItemsResponse.Error>>

//TODO handle conflicts
class SyncBatchProcessor(
    val batches: List<DownloadBatch>,
    val userId: Long,
    val syncApi: SyncApi,
    val dbWrapper: DbWrapper,
    val fileStore: FileStore,
    val itemResponseMapper: ItemResponseMapper,
    val collectionResponseMapper: CollectionResponseMapper,
    val itemResultsUseCase: ItemResultsUseCase,
    val schemaController: SchemaController,
    val completion: (CustomResult<SyncBatchResponse>) -> Unit
) {

    private var failedIds: MutableList<String> = mutableListOf()
    private var parsingErrors: MutableList<Throwable> = mutableListOf()
    private var itemConflicts: MutableList<StoreItemsResponse.Error> = mutableListOf()
    private var isFinished: Boolean = false
    private var processedCount: Int = 0

    suspend fun start() {
        this.batches.map { batch ->
            val keysString = batch.keys.joinToString(separator = ",")
            val url =
                BuildConfig.BASE_API_URL + "/" + batch.libraryId.apiPath(userId = this.userId) + "/" + batch.objectS.apiPath

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

            process(result = networkResult, batch = batch)
        }

    }

    private suspend fun process(result: CustomResult<JsonArray>, batch: DownloadBatch) {
        if (isFinished) {
            return
        }

        when (result) {
            is CustomResult.GeneralSuccess.NetworkSuccess -> {
                val lastModifiedVersion = result.lastModifiedVersion
                val payload = result.value
                process(data = payload, lastModifiedVersion = lastModifiedVersion, batch = batch)
            }
            is CustomResult.GeneralError -> {
                cancel(result)
            }
        }
    }

    private suspend fun process(data: JsonArray, lastModifiedVersion: Int, batch: DownloadBatch) {
        if (this.isFinished) {
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
                userId = this.userId,
                expectedKeys = batch.keys
            )
            //TODO report progress
            finish(response)

        } catch (e: Exception) {
            cancel(CustomResult.GeneralError.CodeError(e))
        }

    }

    private fun finish(response: Triple<List<String>, List<Throwable>, List<StoreItemsResponse.Error>>) {
        if (this.isFinished) {
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
        this.processedCount += 1

        if (this.processedCount == this.batches.size) {
            completion(CustomResult.GeneralSuccess(SyncBatchResponse(this.failedIds, this.parsingErrors, this.itemConflicts)))
            this.isFinished = true
        }

    }

    private suspend fun sync(
        dataArray: JsonArray,
        libraryId: LibraryIdentifier,
        objectS: SyncObject,
        userId: Long,
        expectedKeys: List<String>
    ): SyncBatchResponse {
        return when (objectS) {
            SyncObject.collection -> {
                val collections = dataArray.map {
                    collectionResponseMapper.fromJson(it.asJsonObject)
                }

                val failedKeys =
                    failedKeys(expectedKeys = expectedKeys, parsedKeys = collections.map { it.key })


                SyncBatchResponse(failedKeys, emptyList(), emptyList())
            }
            SyncObject.item, SyncObject.trash -> {
                val objects = mutableListOf<JsonElement>()
                val errors = mutableListOf<Throwable>()
                val items = dataArray.mapNotNull {
                    try {
                        objects.add(it)
                        itemResponseMapper.fromJson(it.asJsonObject)
                    } catch (e :Exception) {
                        Timber.e(e)
                        errors.add(e)
                        null
                    }
                }
                //Set a breakpoint here
                println(items)
                itemResultsUseCase.postResults(items)
                //TODO storeIndividualObjects

//                val request = StoreItemsDbResponseRequest(responses = items, schemaController = this. schemaController, preferResponseData = true)
//                val response = dbWrapper.realmDbStorage.perform(request = request, invalidateRealm = true)
                val failedKeys =
                    failedKeys(expectedKeys = expectedKeys, parsedKeys = items.map { it.key })

//                renameExistingFiles(changes = response.changedFilenames, libraryId = libraryId)
//                SyncBatchResponse(failedKeys, errors, response.conflicts)

                SyncBatchResponse(failedKeys, emptyList(), emptyList())
            }
            SyncObject.settings -> {
                SyncBatchResponse(emptyList(), emptyList(), emptyList())
            }
            else -> {
                SyncBatchResponse(emptyList(), emptyList(), emptyList())
            }

        }
    }

    private fun failedKeys(expectedKeys: List<String>, parsedKeys: List<String>): List<String> {
        return expectedKeys.filter { !parsedKeys.contains(it) }
    }

    private fun renameExistingFiles(changes: List<StoreItemsResponse.FilenameChange>, libraryId: LibraryIdentifier) {
        for (change in changes) {
            val oldFile = fileStore.attachmentFile(libraryId, key = change.key, filename = change.oldName, contentType = change.contentType)
            if (!oldFile.exists()) {
                continue
            }

            val newFile = fileStore.attachmentFile(libraryId, key = change.key, filename = change.newName, contentType =  change.contentType)
            if (!oldFile.renameTo(newFile)) {
                Timber.e("SyncBatchProcessor: can't rename file")
                oldFile.delete()
            }
        }
    }

    private fun cancel(error: CustomResult.GeneralError) {
        //TODO cancel queue
        this.isFinished = true
        completion(error)
    }


}