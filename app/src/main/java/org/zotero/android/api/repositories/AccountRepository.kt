package org.zotero.android.api.repositories

import org.zotero.android.api.AccountApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.api.pojo.login.LoginRequest
import org.zotero.android.architecture.Defaults
import org.zotero.android.sync.SessionController
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val accountApi: AccountApi,
    private val defaults: Defaults,
    private val sessionController: SessionController
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
        sessionController.register(userId = networkResult.value.userId, username = networkResult.value.name,
            displayName = networkResult.value.displayName, apiToken = networkResult.value.key)

        return CustomResult.GeneralSuccess(Unit)
    }
}