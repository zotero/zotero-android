package org.zotero.android.sync.syncactions

import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.database.requests.StoreGroupDbRequest
import org.zotero.android.sync.syncactions.architecture.SyncAction


class FetchAndStoreGroupSyncAction(
    val identifier: Int,
    val userId: Long,
) : SyncAction() {
    suspend fun result(): CustomResult<Unit> {
        val networkResult = safeApiCall {
            syncApi.groupRequest(identifier = identifier)
        }
        if (networkResult !is CustomResult.GeneralSuccess.NetworkSuccess) {
            return networkResult as CustomResult.GeneralError
        }
        dbWrapperMain.realmDbStorage.perform(
            StoreGroupDbRequest(
                response = networkResult.value!!,
                userId = this.userId
            )
        )
        return CustomResult.GeneralSuccess(Unit)
    }
}