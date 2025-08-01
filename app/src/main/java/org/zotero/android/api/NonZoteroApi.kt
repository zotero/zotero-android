package org.zotero.android.api

import com.google.gson.JsonArray
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.HeaderMap
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface NonZoteroApi {

    @POST
    @Multipart
    suspend fun uploadAttachment(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        // must be LinkedHashMap, so order is preserved! Otherwise Amazon won't accept.
        @PartMap params: LinkedHashMap<String,String>,
        @Part file: MultipartBody.Part,
    ): retrofit2.Response<Unit>

    @GET
    @Streaming
    suspend fun downloadFile(@Url url: String): retrofit2.Response<ResponseBody>

    @POST("https://repo.zotero.org/repo/report?debug=1")
    suspend fun debugLogUploadRequest(
        @Body textBody: String,
    ): retrofit2.Response<String?>

    @FormUrlEncoded
    @POST("https://repo.zotero.org/repo/report")
    suspend fun crashLogUploadRequest(
        @Field("error") error: Int = 1,
        @Field("errorData") errorData: String,
        @Field("diagnostic") diagnostic: String,
    ): retrofit2.Response<String?>

    @GET
    suspend fun sendWebViewGet(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
    ): retrofit2.Response<ResponseBody>

    @POST
    suspend fun sendWebViewPost(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body textBody: String?,
    ): retrofit2.Response<ResponseBody>

    @Streaming
    @GET
    suspend fun downloadFileStreaming(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
    ): retrofit2.Response<ResponseBody>

    @HEAD
    suspend fun sendHead(
        @Url url: String,
    ): retrofit2.Response<Void>

    @FormUrlEncoded
    @POST("https://repo.zotero.org/repo/updated")
    suspend fun repoRequest(
        @Query("m") type: Int,
        @Query("last") timestamp: Long,
        @Query("version") version: String,
        @FieldMap fieldMap: Map<String, String>,
    ): retrofit2.Response<ResponseBody>

    @GET("https://www.zotero.org/styles-files/styles.json")
    suspend fun stylesRequest(
    ): retrofit2.Response<JsonArray>
}