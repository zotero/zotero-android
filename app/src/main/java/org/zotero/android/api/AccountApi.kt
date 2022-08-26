package org.zotero.android.api

import org.zotero.android.api.pojo.LoginRequest
import org.zotero.android.api.pojo.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AccountApi {

    @POST("/keys")
    suspend fun loginUser(@Body body: LoginRequest): LoginResponse
}
