package org.zotero.android.api.repositories

import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.api.pojo.login.LoginRequest
import org.zotero.android.sync.SessionController
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val zoteroApi: ZoteroApi,
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
            zoteroApi.loginUser(params)
        }

        if (networkResult !is CustomResult.GeneralSuccess) {
            return networkResult as CustomResult.GeneralError
        }
        sessionController.register(userId = networkResult.value!!.userId, username = networkResult.value!!.name,
            displayName = networkResult.value!!.displayName, apiToken = networkResult.value!!.key)

        return CustomResult.GeneralSuccess(Unit)
    }
}