package org.zotero.android.api

import com.google.gson.JsonObject
import retrofit2.http.DELETE
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {

    @DELETE("/keys/sessions/{token}")
    suspend fun submitCancelLoginSessionRequest(
        @Path("token") token: String,
    ): retrofit2.Response<Unit>

    @GET("/keys/sessions/{token}")
    suspend fun checkLoginSessionRequest(
        @Path("token") token: String,
    ): retrofit2.Response<JsonObject>


    @FormUrlEncoded
    @POST("/keys/sessions")
    suspend fun createLoginSessionRequest(
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @FieldMap fieldMap: Map<String, String> = emptyMap(),
    ): retrofit2.Response<JsonObject>

}
