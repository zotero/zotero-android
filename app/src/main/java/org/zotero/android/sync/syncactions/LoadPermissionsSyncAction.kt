package org.zotero.android.sync.syncactions

import com.google.gson.Gson
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.sync.syncactions.data.KeyResponse

class LoadPermissionsSyncAction @AssistedInject constructor(
    private val zoteroApi: ZoteroApi,
    private val gson: Gson
) {

    suspend fun result(): CustomResult<KeyResponse> {
        val networkResult = safeApiCall {
            zoteroApi.getKeys()
        }

        if (networkResult !is CustomResult.GeneralSuccess) {
            return networkResult as CustomResult.GeneralError
        }

        val keyResponse = KeyResponse.fromJson(data = networkResult.value!!, gson = gson)
        return CustomResult.GeneralSuccess(keyResponse)
    }

    @AssistedFactory
    interface Factory {
        fun create(): LoadPermissionsSyncAction
    }

}