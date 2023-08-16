package org.zotero.android.sync.syncactions

import com.google.gson.JsonObject
import kotlinx.coroutines.withContext
import org.zotero.android.BuildConfig
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.api.pojo.sync.CollectionResponse
import org.zotero.android.api.pojo.sync.FailedUpdateResponse
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.SearchResponse
import org.zotero.android.api.pojo.sync.UpdatesResponse
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RSearch
import org.zotero.android.database.requests.MarkCollectionAsSyncedAndUpdateDbRequest
import org.zotero.android.database.requests.MarkForResyncDbAction
import org.zotero.android.database.requests.MarkItemAsSyncedAndUpdateDbRequest
import org.zotero.android.database.requests.MarkObjectsAsSyncedDbRequest
import org.zotero.android.database.requests.MarkSearchAsSyncedAndUpdateDbRequest
import org.zotero.android.database.requests.SplitAnnotationsDbRequest
import org.zotero.android.database.requests.UpdateVersionType
import org.zotero.android.database.requests.UpdateVersionsDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncActionError
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.syncactions.architecture.SyncAction
import timber.log.Timber
import java.io.FileWriter

class SubmitUpdateSyncAction(
    val parameters: List<Map<String, Any>>,
    val changeUuids: Map<String, List<String>>,
    val sinceVersion: Int?,
    val objectS: SyncObject,
    val libraryId: LibraryIdentifier,
    val userId: Long,
    val updateLibraryVersion: Boolean,
) : SyncAction() {
    private val splitMessage = "Annotation position is too long"

    suspend fun result(): CustomResult<Pair<Int, CustomResult.GeneralError.CodeError?>> {
        return withContext(dispatcher) {
            try {
                when (this@SubmitUpdateSyncAction.objectS) {
                    SyncObject.settings ->
                        TODO()
                    SyncObject.collection, SyncObject.item, SyncObject.search, SyncObject.trash ->
                        return@withContext submitOther()
                }
            } catch (e:Exception) {
                Timber.e(e, "SubmitUpdateSyncAction: can't parse updates response")
                return@withContext CustomResult.GeneralError.CodeError(e)
            }
        }
    }

    private suspend fun submitOther(): CustomResult<Pair<Int, CustomResult.GeneralError.CodeError?>> {
        val objectType = this.objectS
        val url =
            BuildConfig.BASE_API_URL + "/" + this.libraryId.apiPath(userId = this.userId) + "/" + objectType.apiPath

        val networkResult = safeApiCall {
            val jsonBody: String
            when (objectType) {
                SyncObject.settings ->
                    jsonBody = gson.toJson(this.parameters.first())
                else ->
                    jsonBody = gson.toJson(this.parameters)
            }

            val headers = mutableMapOf<String, String>()
            this.sinceVersion?.let {
                headers.put("If-Unmodified-Since-Version", it.toString())
            }
            syncApi.updates(url = url, jsonBody = jsonBody, headers = headers)
        }

        if (networkResult !is CustomResult.GeneralSuccess) {
            return networkResult as CustomResult.GeneralError
        }
        networkResult as CustomResult.GeneralSuccess.NetworkSuccess
        val newVersion = networkResult.lastModifiedVersion
        val json = networkResult.value!!
        val keys = this.parameters.map{ it["key"]?.toString() }
        val updatesResponse = updatesResponseMapper.fromJson(dictionary = json, keys = keys)
        return process(response = updatesResponse, newVersion = newVersion)
    }

    private fun process(response: UpdatesResponse, newVersion: Int): CustomResult<Pair<Int, CustomResult.GeneralError.CodeError?>> {
        val requests = createRequests(response = response, version = newVersion, updateLibraryVersion = this.updateLibraryVersion)

        if (!response.successfulJsonObjects.isEmpty()) {
            when (this.objectS) {
                SyncObject.item, SyncObject.trash ->
                    storeIndividualItemJsonObjects(response.successfulJsonObjects.values, libraryId = this.libraryId)
                SyncObject.collection, SyncObject.search, SyncObject.settings -> {
                    //no-op
                }
            }
        }

        if (!requests.isEmpty()) {
            try {
                dbWrapper.realmDbStorage.perform(requests)
            } catch (e:Exception) {
                Timber.e(e, "SubmitUpdateSyncAction: can't store local changes")
                return CustomResult.GeneralSuccess(Pair(newVersion, CustomResult.GeneralError.CodeError(e)))
            }
        }

        val error = process(failedResponses = response.failed, this.libraryId)
        return CustomResult.GeneralSuccess(Pair(newVersion, error?.let { CustomResult.GeneralError.CodeError(error) }))
    }

    private fun createRequests(response: UpdatesResponse, version: Int, updateLibraryVersion: Boolean): List<DbRequest> {
        val (unchangedKeys, parsingFailedKeys, changedCollections, changedItems, changedSearches) = process(response = response)

        var requests = mutableListOf<DbRequest>()

        if (!unchangedKeys.isEmpty()) {
            when (this.objectS) {
                SyncObject.collection ->
                    requests.add(MarkObjectsAsSyncedDbRequest(libraryId = this.libraryId, keys = unchangedKeys,
                        changeUuids = this.changeUuids, version = version, clazz = RCollection::class.java))
                SyncObject.item, SyncObject.trash ->
                    requests.add(MarkObjectsAsSyncedDbRequest(libraryId = this.libraryId, keys = unchangedKeys,
                        changeUuids = this.changeUuids, version = version, clazz = RItem::class.java))
                SyncObject.search ->
                    requests.add(MarkObjectsAsSyncedDbRequest(libraryId = this.libraryId, keys = unchangedKeys,
                        changeUuids = this.changeUuids, version = version, clazz = RSearch::class.java))
                SyncObject.settings -> {
                    //no-op
                }
            }
        }

        if (!parsingFailedKeys.isEmpty()) {
            when (this.objectS) {
                SyncObject.collection ->
                    requests.add(MarkForResyncDbAction(libraryId = this.libraryId, keys = unchangedKeys, clazz = RCollection::class))
                SyncObject.item, SyncObject.trash ->
                    requests.add(MarkForResyncDbAction(libraryId = this.libraryId, keys = unchangedKeys, clazz = RItem::class))
                SyncObject.search ->
                    requests.add(MarkForResyncDbAction(libraryId = this.libraryId, keys = unchangedKeys, clazz = RSearch::class))
                SyncObject.settings -> {
                    //no-op
                }
            }
        }

        if (!changedCollections.isEmpty()) {
            for (response in changedCollections) {
                val changeUuids = this.changeUuids[response.key] ?: emptyList()
                requests.add(MarkCollectionAsSyncedAndUpdateDbRequest(libraryId = this.libraryId,
                    response = response, changeUuids = changeUuids))
            }
        }

        if (!changedItems.isEmpty()) {
            for (response in changedItems) {
                val changeUuids = this.changeUuids[response.key] ?: emptyList()
                requests.add(
                    MarkItemAsSyncedAndUpdateDbRequest(
                        libraryId = this.libraryId,
                        response = response,
                        changeUuids = changeUuids,
                        schemaController = this.schemaController,
                        dateParser = this.dateParser
                    )
                )
            }
        }

        if (!changedSearches.isEmpty()) {
            for (response in changedSearches) {
                val changeUuids = this.changeUuids[response.key] ?: emptyList()
                requests.add(
                    MarkSearchAsSyncedAndUpdateDbRequest(
                        libraryId = this.libraryId,
                        response = response,
                        changeUuids = changeUuids
                    )
                )
            }
        }

        if (updateLibraryVersion) {
            requests.add(UpdateVersionsDbRequest(version = version, libraryId = this.libraryId,
                type = UpdateVersionType.objectS(this.objectS)))
        }

        return requests
    }

    private fun process(failedResponses: List<FailedUpdateResponse>, libraryId: LibraryIdentifier): Exception? {
        if(failedResponses.isEmpty()) {
            return null
        }

        var splitKeys = mutableSetOf<String>()

        for (response in failedResponses) {
            when (response.code) {
                412 -> {
                    Timber.e("SubmitUpdateSyncAction: failed ${response.key ?: "unknown key"} " +
                            "- ${response.message}. Library ${libraryId}")
                    return SyncActionError.objectPreconditionError
                }
                400 -> {
                    if (response.message.contains(this.splitMessage) && response.key != null) {
                        splitKeys.add(response.key)
                    }
                }
                else -> {
                    continue
                }
            }
        }

        if (!splitKeys.isEmpty()) {
            Timber.w("SubmitUpdateSyncAction: annotations too long: ${splitKeys} in ${libraryId}")

            try {
                dbWrapper.realmDbStorage.perform(request = SplitAnnotationsDbRequest(keys = splitKeys, libraryId = libraryId))
                    return SyncActionError.annotationNeededSplitting(messageS = this.splitMessage, keys = splitKeys, libraryId = libraryId)
                } catch (e:Exception) {
                    Timber.e(e, "SubmitUpdateSyncAction: could not split annotations")
                }
            }

        Timber.e("SubmitUpdateSyncAction: failures - ${failedResponses}")

        val errorMessages = failedResponses.map{ it.message }.joinToString(separator = "\n")
        return SyncActionError.submitUpdateFailures(errorMessages)
    }

    private fun process(response: UpdatesResponse): SubmitUpdateProcessResponse {
        var unchangedKeys = response.unchanged.values.toMutableList()
        var changedCollections = mutableListOf<CollectionResponse>()
        var changedItems = mutableListOf<ItemResponse>()
        var changedSearches = mutableListOf<SearchResponse>()
        var parsingFailedKeys = mutableListOf<String>()


        for((idx, json) in response.successfulJsonObjects) {
            val key = response.successful[idx]
            if (key == null) {
                continue
            }
            try {
                when (this.objectS) {
                    SyncObject.collection -> {
                        changedCollections.add(collectionResponseMapper.fromJson(json))
                    }

                    SyncObject.item, SyncObject.trash -> {
                        changedItems.add(itemResponseMapper.fromJson(json, this.schemaController))
                    }

                    SyncObject.search -> {
                        changedSearches.add(searchResponseMapper.fromJson(json))
                    }

                    SyncObject.settings -> {
                        //no-op
                    }
                }
            } catch (e:Exception) {
                Timber.e(e, "SubmitUpdateSyncAction: could not parse json for object ${this.objectS}")
                unchangedKeys.add(key)
                parsingFailedKeys.add(key)
            }
        }

        return SubmitUpdateProcessResponse(unchangedKeys, parsingFailedKeys, changedCollections, changedItems, changedSearches)
    }

    private fun storeIndividualItemJsonObjects(jsonObjects: Collection<JsonObject>, libraryId: LibraryIdentifier) {
        for (obj in jsonObjects) {
            val objectS = obj.asJsonObject
            val key = objectS["key"]?.asString ?: continue
            try {
                val file = fileStore.jsonCacheFile(SyncObject.item, libraryId = libraryId, key = key)
                val fileWriter = FileWriter(file)
                gson.toJson(objectS, fileWriter)
                fileWriter.flush()
                fileWriter.close()
                println("")
            } catch (e:Throwable) {
                Timber.e(e, "SubmitUpdateSyncAction: can't encode/write item - $objectS")
            }
        }
    }

}

private data class SubmitUpdateProcessResponse(
    val unchangedKeys: List<String>,
    val parsingFailedKeys: List<String>,
    val changedCollections: List<CollectionResponse>,
    val changedItems: List<ItemResponse>,
    val changedSearches: List<SearchResponse>
)