package org.zotero.android.sync

import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.Defaults
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.SyncGroupVersionsDbRequest
import javax.inject.Inject

class SyncRepository @Inject constructor(
    private val syncApi: SyncApi,
    private val dbWrapperMain: DbWrapperMain,
    private val defaults: Defaults,
) {
    suspend fun processSyncGroupVersions(): CustomResult<Pair<List<Int>, List<Pair<Int, String>>>> {
        val networkResult = safeApiCall {
            syncApi.groupVersionsRequest(userId = defaults.getUserId())
        }

        if (networkResult !is CustomResult.GeneralSuccess) {
            return networkResult as CustomResult.GeneralError
        }

        val dbRes: Pair<List<Int>, List<Pair<Int, String>>> = dbWrapperMain.realmDbStorage.perform(
            request = SyncGroupVersionsDbRequest(versions = networkResult.value!!),
            invalidateRealm = true
        )

        return CustomResult.GeneralSuccess(dbRes)
    }


}