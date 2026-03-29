package org.zotero.android.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.HeaderMap
import retrofit2.http.Url

interface NonZoteroNoRedirectApi {
    @GET
    suspend fun sendWebViewGet(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
    ): retrofit2.Response<ResponseBody>

    @HEAD
    suspend fun sendHead(
        @Url url: String,
    ): retrofit2.Response<Void>
}
