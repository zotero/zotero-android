package org.zotero.android.sync.syncactions

import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.requests.StoreGroupDbRequest
import org.zotero.android.sync.SyncAction

class FetchAndStoreGroupSyncAction(
    val dbStorage: DbWrapper,
    val syncApi: SyncApi,
    val identifier: Int,
    val userId: Long,
) : SyncAction<CustomResult<Unit>> {
    override suspend fun result(): CustomResult<Unit> {
        val networkResult = safeApiCall {
            syncApi.groupRequest(identifier = identifier)
        }
        if (networkResult !is CustomResult.GeneralSuccess.NetworkSuccess) {
            return networkResult as CustomResult.GeneralError
        }
        dbStorage.realmDbStorage.perform(
            StoreGroupDbRequest(
                response = networkResult.value!!,
                userId = this.userId
            )
        )
        return CustomResult.GeneralSuccess(Unit)
    }
}