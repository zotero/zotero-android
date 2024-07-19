package org.zotero.android.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.HeaderMap
import retrofit2.http.PUT
import retrofit2.http.Streaming
import retrofit2.http.Url

@JvmSuppressWildcards
interface WebDavApi {

    @GET
    @Streaming
    suspend fun downloadFile(@Url url: String): retrofit2.Response<ResponseBody>

    @HTTP(method = "OPTIONS")
    suspend fun options(@Url url: String): retrofit2.Response<ResponseBody>

    @HTTP(method = "MKCOL")
    suspend fun mkcol(@Url url: String): retrofit2.Response<ResponseBody>

    @HTTP(method = "PROPFIND", hasBody = true)
    suspend fun propfind(
        @Url url: String,
        @Body body: RequestBody,
        @HeaderMap headers: Map<String, String>,
    ): retrofit2.Response<ResponseBody>

    @HTTP(method = "GET")
    suspend fun get(@Url url: String): retrofit2.Response<ResponseBody>

    @PUT
    suspend fun put(
        @Url url: String,
        @Body body: RequestBody,
    ): retrofit2.Response<ResponseBody>

    @HTTP(method = "DELETE")
    suspend fun delete(@Url url: String): retrofit2.Response<ResponseBody>

    @HTTP(method = "DELETE")
    fun deleteSync(@Url url: String): Call<ResponseBody>

    @HTTP(method = "PUT", hasBody = true)
    suspend fun uploadProp(
        @Url url: String,
        @Body body: RequestBody,
    ): retrofit2.Response<ResponseBody>

    @PUT
    suspend fun uploadAttachment(
        @Url url: String,
        @Body body: RequestBody,
    ): retrofit2.Response<Unit>
}