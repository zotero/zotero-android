package org.zotero.android.api.repositories

import org.zotero.android.api.AccountApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.api.pojo.login.LoginRequest
import org.zotero.android.architecture.SdkPrefs
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val accountApi: AccountApi,
    private val sdkPrefs: SdkPrefs
) {

    suspend fun login(
        username: String,
        password: String,
    ): CustomResult<Unit> {

        val networkResult = safeApiCall {
            val params = LoginRequest(
                username = username,
                password = password,
            )
            accountApi.loginUser(params)
        }

        if (networkResult !is CustomResult.GeneralSuccess) {
            return networkResult as CustomResult.GeneralError
        }
        sdkPrefs.setUserId(networkResult.value.userId)
        sdkPrefs.setDisplayName(networkResult.value.displayName)
        sdkPrefs.setName(networkResult.value.name)
        sdkPrefs.setApiToken(networkResult.value.key)
        return CustomResult.GeneralSuccess(Unit)
    }
}