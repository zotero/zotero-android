package org.zotero.android.api

import retrofit2.http.GET
import retrofit2.http.Url

interface NonZoteroNoRedirectApi {
    @GET
    suspend fun sendGet(@Url url: String): retrofit2.Response<Void>

}
