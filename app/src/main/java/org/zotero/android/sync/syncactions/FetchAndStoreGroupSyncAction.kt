package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.StoreGroupDbRequest


class FetchAndStoreGroupSyncAction @AssistedInject constructor(
    @Assisted("identifier") private val identifier: Int,
    @Assisted("userId") private val userId: Long,

    private val zoteroApi: ZoteroApi,
    private val dbWrapperMain: DbWrapperMain,
) {
    suspend fun result(): CustomResult<Unit> {
        val networkResult = safeApiCall {
            zoteroApi.groupRequest(identifier = identifier)
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


    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("identifier") identifier: Int,
            @Assisted("userId") userId: Long
        ): FetchAndStoreGroupSyncAction
    }
}