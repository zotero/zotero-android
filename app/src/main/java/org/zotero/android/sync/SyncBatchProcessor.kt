package org.zotero.android.sync

import com.google.gson.JsonArray
import org.zotero.android.BuildConfig
import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.data.mappers.ItemResponseMapper
import org.zotero.android.files.FileStore

typealias SyncBatchResponse = Triple<List<String>, List<Throwable>, List<StoreItemsResponse.Error>>

//TODO handle conflicts
class SyncBatchProcessor(
    val batches: List<DownloadBatch>,
    val userId: Long,
    val syncApi: SyncApi,
    val dbWrapper: DbWrapper,
    val fileStore: FileStore,
    val itemResponseMapper: ItemResponseMapper,
    val itemResultsUseCase: ItemResultsUseCase,
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

    private fun process(result: CustomResult<JsonArray>, batch: DownloadBatch) {
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

    private fun process(data: JsonArray, lastModifiedVersion: Int, batch: DownloadBatch) {
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

    private fun sync(
        dataArray: JsonArray,
        libraryId: LibraryIdentifier,
        objectS: SyncObject,
        userId: Long,
        expectedKeys: List<String>
    ): SyncBatchResponse {
        return when (objectS) {
            SyncObject.item, SyncObject.trash -> {
                val items = dataArray.map {
                    itemResponseMapper.fromJson(it.asJsonObject)
                }
                //Set a breakpoint here
                println(items)
                itemResultsUseCase.postResults(items)

                val failedKeys =
                    failedKeys(expectedKeys = expectedKeys, parsedKeys = items.map { it.key })

                //TODO return parse errors and conflicts
                SyncBatchResponse(failedKeys, emptyList(), emptyList())
            }
            else -> {
                SyncBatchResponse(emptyList(), emptyList(), emptyList())
            }
        }
    }

    private fun failedKeys(expectedKeys: List<String>, parsedKeys: List<String>): List<String> {
        return expectedKeys.filter { !parsedKeys.contains(it) }
    }

    private fun cancel(error: CustomResult.GeneralError) {
        //TODO cancel queue
        this.isFinished = true
        completion(error)
    }


}