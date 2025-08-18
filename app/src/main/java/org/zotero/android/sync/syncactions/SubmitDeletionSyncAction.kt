package org.zotero.android.sync.syncactions

import org.zotero.android.BuildConfig
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RSearch
import org.zotero.android.database.requests.CreateWebDavDeletionsDbRequest
import org.zotero.android.database.requests.DeleteObjectsDbRequest
import org.zotero.android.database.requests.UpdateVersionType
import org.zotero.android.database.requests.UpdateVersionsDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.syncactions.architecture.SyncAction
import timber.log.Timber

class SubmitDeletionSyncAction(
    val keys: List<String>,
    val objectS: SyncObject,
    val version: Int,
    val libraryId: LibraryIdentifier,
    val userId: Long,
    val webDavEnabled: Boolean,
) : SyncAction() {
    suspend fun result(): CustomResult<Pair<Int, Boolean>> {
        val url =
            BuildConfig.BASE_API_URL + "/" + this.libraryId.apiPath(userId = this.userId) + "/" + this.objectS.apiPath
        val networkResult = safeApiCall {
            val joinedKeys = this.keys.joinToString(separator = ",")

            val parameters = mutableMapOf<String, String>()
            when (this.objectS) {
                SyncObject.collection ->
                    parameters["collectionKey"] = joinedKeys

                SyncObject.item, SyncObject.trash ->
                    parameters["itemKey"] = joinedKeys

                SyncObject.search ->
                    parameters["searchKey"] = joinedKeys

                SyncObject.settings -> {}
            }
            zoteroApi.submitDeletionsRequest(
                url = url,
                queryMap = parameters,
                headers = mapOf("If-Modified-Since-Version" to this.version.toString())
            )
        }
        if (networkResult !is CustomResult.GeneralSuccess.NetworkSuccess) {
            return networkResult as CustomResult.GeneralError
        }
        val lastModifiedVersion = networkResult.lastModifiedVersion
        val deleteResult = deleteFromDb(version = lastModifiedVersion)
        return CustomResult.GeneralSuccess(lastModifiedVersion to deleteResult)
    }

    private fun deleteFromDb(version: Int): Boolean {
        try {
            var didCreateDeletions = false
            dbWrapperMain.realmDbStorage.perform { coordinator ->
                val updateVersion = UpdateVersionsDbRequest(
                    version = version,
                    libraryId = this.libraryId,
                    type = UpdateVersionType.objectS(this.objectS)
                )
                val requests = mutableListOf<DbRequest>(updateVersion)

                when (this.objectS) {
                    SyncObject.collection -> {
                        requests.add(
                            element = DeleteObjectsDbRequest(
                                keys = this.keys,
                                libraryId = this.libraryId,
                                clazz = RCollection::class
                            ), index = 0
                        )
                    }

                    SyncObject.item, SyncObject.trash -> {
                        requests.add(
                            element = DeleteObjectsDbRequest(
                                keys = this.keys,
                                libraryId = this.libraryId,
                                clazz = RItem::class
                            ), index = 0
                        )
                        if (this.webDavEnabled) {
                            didCreateDeletions = coordinator.perform(
                                request = CreateWebDavDeletionsDbRequest(
                                    keys = this.keys,
                                    libraryId = this.libraryId
                                )
                            )
                        }
                    }

                    SyncObject.search -> {
                        requests.add(
                            element = DeleteObjectsDbRequest(
                                keys = this.keys,
                                libraryId = this.libraryId,
                                clazz = RSearch::class
                            ), index = 0
                        )
                    }

                    SyncObject.settings -> {
                        //no-op
                    }
                }
                coordinator.perform(requests)
                coordinator.invalidate()
            }
            return didCreateDeletions
        } catch (error: Exception) {
            Timber.e(error, "SubmitDeletionSyncAction: can't delete objects")
            throw error
        }

    }

}