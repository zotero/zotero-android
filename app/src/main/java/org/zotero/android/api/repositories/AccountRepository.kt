package org.zotero.android.api.repositories

import org.zotero.android.api.AccountApi
import org.zotero.android.api.network.NetworkResultWrapper
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.api.pojo.LoginRequest
import org.zotero.android.architecture.SdkPrefs
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val accountApi: AccountApi,
    private val sdkPrefs: SdkPrefs
) {

    suspend fun login(
        username: String,
        password: String,
    ): NetworkResultWrapper<Unit> {

        val networkResult = safeApiCall {
            val params = LoginRequest(
                username = username,
                password = password,
            )
            accountApi.loginUser(params)
        }

        if (networkResult !is NetworkResultWrapper.Success) {
            return networkResult as NetworkResultWrapper.NetworkError
        }
        sdkPrefs.setUserId(networkResult.value.userId)
        sdkPrefs.setDisplayName(networkResult.value.displayName)
        sdkPrefs.setName(networkResult.value.name)
        sdkPrefs.setApiToken(networkResult.value.key)
        return NetworkResultWrapper.Success(Unit)
    }

}