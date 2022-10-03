package org.zotero.android.api

import org.zotero.android.api.pojo.login.LoginRequest
import org.zotero.android.api.pojo.login.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AccountApi {

    @POST("/keys")
    suspend fun loginUser(@Body body: LoginRequest): retrofit2.Response<LoginResponse>
}
