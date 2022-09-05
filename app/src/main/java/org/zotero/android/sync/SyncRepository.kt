package org.zotero.android.sync

import SyncGroupVersionsDbRequest
import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.NetworkResultWrapper
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.SdkPrefs
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.data.KeyResponse
import javax.inject.Inject

class SyncRepository @Inject constructor(
    private val syncApi: SyncApi,
    private val dbWrapper: DbWrapper,
    private val sdkPrefs: SdkPrefs
) {
    suspend fun processKeyCheckAction(): NetworkResultWrapper<KeyResponse> {
        val networkResult = safeApiCall {
            syncApi.getKeys()
        }

        if (networkResult !is NetworkResultWrapper.Success) {
            return networkResult as NetworkResultWrapper.NetworkError
        }

        val keyResponse = KeyResponse.fromJson(networkResult.value)

        sdkPrefs.setUsername( keyResponse.username)
        sdkPrefs.setDisplayName( keyResponse.displayName)

        return NetworkResultWrapper.Success(keyResponse)
    }

    suspend fun processSyncGroupVersions(): NetworkResultWrapper<Pair<List<Int>, List<Pair<Int, String>>>> {
        val networkResult = safeApiCall {
            syncApi.groupVersionsRequest(userId = sdkPrefs.getUserId())
        }

        if (networkResult !is NetworkResultWrapper.Success) {
            return networkResult as NetworkResultWrapper.NetworkError
        }

        val dbRes: Pair<List<Int>, List<Pair<Int, String>>> = dbWrapper.realmDbStorage.perform(
            request = SyncGroupVersionsDbRequest(versions = networkResult.value),
            invalidateRealm = true
        )



        return NetworkResultWrapper.Success(dbRes)
    }


}