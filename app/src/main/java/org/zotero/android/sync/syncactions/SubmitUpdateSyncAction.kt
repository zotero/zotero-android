package org.zotero.android.sync.syncactions

import org.zotero.android.BuildConfig
import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.api.pojo.sync.CollectionResponse
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.SearchResponse
import org.zotero.android.api.pojo.sync.UpdatesResponse
import org.zotero.android.architecture.database.DbRequest
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.objects.RCollection
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.RSearch
import org.zotero.android.architecture.database.requests.MarkCollectionAsSyncedAndUpdateDbRequest
import org.zotero.android.architecture.database.requests.MarkForResyncDbAction
import org.zotero.android.architecture.database.requests.MarkItemAsSyncedAndUpdateDbRequest
import org.zotero.android.architecture.database.requests.MarkObjectsAsSyncedDbRequest
import org.zotero.android.data.mappers.CollectionResponseMapper
import org.zotero.android.data.mappers.ItemResponseMapper
import org.zotero.android.data.mappers.SearchResponseMapper
import org.zotero.android.data.mappers.UpdatesResponseMapper
import org.zotero.android.files.FileStore
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SyncActionWithError
import org.zotero.android.sync.SyncObject
import timber.log.Timber

class SubmitUpdateSyncAction(
    val parameters:List<Map<String, Any>>,
    val changeUuids:Map<String, List<String>>,
    val sinceVersion:Int?,
    val objectS:SyncObject,
    val libraryId:LibraryIdentifier,
    val userId:Long,
    val updateLibraryVersion:Boolean,
    val syncApi:SyncApi,
    val dbStorage:DbWrapper,
    val fileStorage:FileStore,
    val schemaController:SchemaController,
    val updatesResponseMapper: UpdatesResponseMapper,
    val collectionResponseMapper: CollectionResponseMapper,
    val itemResponseMapper: ItemResponseMapper,
    val searchResponseMapper: SearchResponseMapper,
) : SyncActionWithError<Pair<Int, Exception>> {
    private val splitMessage = "Annotation position is too long"


    override suspend fun result(): CustomResult<Pair<Int, Exception>> {
        try {
            when (this.objectS) {
                SyncObject.settings ->
                    TODO()
                SyncObject.collection, SyncObject.item, SyncObject.search, SyncObject.trash ->
                    return submitOther()
            }
        } catch (e :Exception) {
            Timber.e(e, "SubmitUpdateSyncAction: can't parse updates response")
            return CustomResult.GeneralError.CodeError(e)
        }
    }

    private suspend fun submitOther(): CustomResult<Pair<Int, Exception>> {
        val objectType = this.objectS
        val url =
            BuildConfig.BASE_API_URL + "/" + this.libraryId.apiPath(userId = this.userId) + "/" + objectType.apiPath

        val networkResult = safeApiCall {
            val parameters: Map<String, Any>
            when (objectType) {
                SyncObject.settings ->
                    parameters = this.parameters.first()
                else ->
                    parameters = mapOf("arrayParametersKey" to this.parameters)
            }

            val headers = mutableMapOf<String, String>()
            this.sinceVersion?.let {
                headers.put("If-Unmodified-Since-Version", it.toString())
            }
            syncApi.updates(url = url, fieldMap = parameters, headers = headers)
        }

        if (networkResult !is CustomResult.GeneralSuccess) {
            return networkResult as CustomResult.GeneralError
        }
        networkResult as CustomResult.GeneralSuccess.NetworkSuccess
        val newVersion = networkResult.lastModifiedVersion
        val json = networkResult.value
        val keys = this.parameters.map{ it["key"]?.toString() }
        val updatesResponse = updatesResponseMapper.fromJson(dictionary = json, keys = keys)
        return process(response = updatesResponse, newVersion = newVersion)
    }

    private fun process(response: UpdatesResponse, newVersion: Int): CustomResult<Pair<Int, Exception>> {
        TODO()

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
                    requests.add(MarkForResyncDbAction(libraryId = this.libraryId, keys = unchangedKeys, clazz = RCollection::class.java))
                SyncObject.item, SyncObject.trash ->
                    requests.add(MarkForResyncDbAction(libraryId = this.libraryId, keys = unchangedKeys, clazz = RItem::class.java))
                SyncObject.search ->
                    requests.add(MarkForResyncDbAction(libraryId = this.libraryId, keys = unchangedKeys, clazz = RSearch::class.java))
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
                    MarkItemAsSyncedAndUpdateDbRequest(libraryId = this.libraryId, response = response,
                    changeUuids = changeUuids, schemaController = this.schemaController)
                )
            }
        }
//
//        if !changedSearches.isEmpty {
//            // Update searches locally based on response from backend and mark as submitted.
//            for response in changedSearches {
//                let changeUuids = self.changeUuids[response.key] ?? []
//                requests.append(MarkSearchAsSyncedAndUpdateDbRequest(libraryId: self.libraryId, response: response, changeUuids: changeUuids))
//            }
//        }
//
//        if updateLibraryVersion {
//            requests.append(UpdateVersionsDbRequest(version: version, libraryId: self.libraryId, type: .object(self.object)))
//        }

        return requests
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
                        val response = collectionResponseMapper.fromJson(json)
                        changedCollections.add(response)
                    }

                    SyncObject.item, SyncObject.trash -> {
                        val response = itemResponseMapper.fromJson(json, this.schemaController)
                        changedItems.add(response)
                    }

                    SyncObject.search -> {
                        val response = searchResponseMapper.fromJson(json)
                        changedSearches.add(response)
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

}

private data class SubmitUpdateProcessResponse(
    val unchangedKeys: List<String>,
    val parsingFailedKeys: List<String>,
    val changedCollections: List<CollectionResponse>,
    val changedItems: List<ItemResponse>,
    val changedSearches: List<SearchResponse>
)