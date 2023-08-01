package org.zotero.android.api

import retrofit2.http.GET
import retrofit2.http.Url

@JvmSuppressWildcards
interface NoRedirectApi {

    @GET
    suspend fun getRequestHeadersApi(@Url url: String): retrofit2.Response<Void>

}
