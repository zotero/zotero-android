package org.zotero.android.sync

import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.requests.SyncGroupVersionsDbRequest
import org.zotero.android.data.AccessPermissions
import org.zotero.android.data.KeyResponse
import javax.inject.Inject

class SyncRepository @Inject constructor(
    private val syncApi: SyncApi,
    private val dbWrapper: DbWrapper,
    private val defaults: Defaults
) {
    suspend fun processKeyCheckAction(): CustomResult<AccessPermissions> {
        val networkResult = safeApiCall {
            syncApi.getKeys()
        }

        if (networkResult !is CustomResult.GeneralSuccess) {
            return networkResult as CustomResult.GeneralError
        }

        val keyResponse = KeyResponse.fromJson(networkResult.value)

        defaults.setUsername( keyResponse.username)
        defaults.setDisplayName( keyResponse.displayName)

        return CustomResult.GeneralSuccess(
            AccessPermissions(
                user = keyResponse.user,
                groupDefault = keyResponse.defaultGroup,
                groups = keyResponse.groups
            )
        )
    }

    suspend fun processSyncGroupVersions(): CustomResult<Pair<List<Int>, List<Pair<Int, String>>>> {
        val networkResult = safeApiCall {
            syncApi.groupVersionsRequest(userId = defaults.getUserId())
        }

        if (networkResult !is CustomResult.GeneralSuccess) {
            return networkResult as CustomResult.GeneralError
        }

        val dbRes: Pair<List<Int>, List<Pair<Int, String>>> = dbWrapper.realmDbStorage.perform(
            request = SyncGroupVersionsDbRequest(versions = networkResult.value),
            invalidateRealm = true
        )



        return CustomResult.GeneralSuccess(dbRes)
    }


}