package org.zotero.android.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Url

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


}
