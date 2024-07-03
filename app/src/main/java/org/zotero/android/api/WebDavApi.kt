package org.zotero.android.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

@JvmSuppressWildcards
interface WebDavApi {

    @GET
    @Streaming
    suspend fun downloadFile(@Url url: String): retrofit2.Response<ResponseBody>

    @Streaming
    @GET
    suspend fun downloadFileStreaming(
        @Url url: String,
    ): retrofit2.Response<ResponseBody>
}