package org.zotero.android.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Url

@JvmSuppressWildcards
interface SyncApi {

    @GET("/keys/current")
    suspend fun getKeys(): retrofit2.Response<JsonObject>

    @GET("users/{userId}/groups")
    suspend fun groupVersionsRequest(
        @Path("userId") userId: Long,
        @Query("format") format: String = "versions",
    ): retrofit2.Response<Map<Int, Int>>

    @GET
    suspend fun versions(
        @Url url: String,
        @Query("format") format: String = "versions",
        @Query("since") since: Int? = null
    ): retrofit2.Response<Map<String, Int>>

    @GET
    suspend fun objects(
        @Url url: String,
        @QueryMap queryMap: Map<String, String>
    ): retrofit2.Response<JsonArray>

    @FormUrlEncoded
    @POST("{basePath}/items/{key}/file")
    fun authorizeUpload(
        @HeaderMap headers: Map<String, String>,
        @Path("basePath") basePath: String,
        @Path("key") key: String,
        @Field("filename") filename: String,
        @Field("filesize") filesize: Long,
        @Field("md5") md5: String,
        @Field("mtime") mtime: Long,
        @Field("params") params: Int): retrofit2.Response<JsonObject>

    @POST
    fun uploadAttachment(
        @Body requestBody: RequestBody,
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): retrofit2.Response<Unit>

    @FormUrlEncoded
    @POST("{basePath}/items/{key}/file")
    fun registerUpload(
        @HeaderMap headers: Map<String, String>,
        @Path("basePath") basePath: String,
        @Path("key") key: String,
        @Field("upload") upload: String,
    ) : retrofit2.Response<JsonObject>

    @POST
    suspend fun updates(
        @Url url: String,
        @Body jsonBody:String,
        @HeaderMap headers: Map<String, String>
    ): retrofit2.Response<JsonObject>

}
