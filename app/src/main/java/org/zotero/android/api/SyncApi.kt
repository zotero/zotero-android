package org.zotero.android.api

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SyncApi {

    @GET("/keys/current")
    suspend fun getKeys(): JsonObject

    @GET("users/{userId}/groups")
    suspend fun groupVersionsRequest(
        @Path("userId") userId: Long,
        @Query("format") format: String = "versions",
    ): Map<Int, Int>

}
