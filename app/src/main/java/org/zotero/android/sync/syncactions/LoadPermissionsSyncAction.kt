package org.zotero.android.sync.syncactions

import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.sync.syncactions.architecture.SyncAction
import org.zotero.android.sync.syncactions.data.KeyResponse

class LoadPermissionsSyncAction : SyncAction() {

    suspend fun result(): CustomResult<KeyResponse> {
        val networkResult = safeApiCall {
            syncApi.getKeys()
        }

        if (networkResult !is CustomResult.GeneralSuccess) {
            return networkResult as CustomResult.GeneralError
        }

        val keyResponse = KeyResponse.fromJson(data = networkResult.value!!, gson = gson)
        return CustomResult.GeneralSuccess(keyResponse)
    }

}