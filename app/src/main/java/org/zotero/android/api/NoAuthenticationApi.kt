package org.zotero.android.api

import okhttp3.MultipartBody
import retrofit2.http.HeaderMap
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Url

@JvmSuppressWildcards
interface NoAuthenticationApi {
    @POST
    @Multipart
    suspend fun uploadAttachment(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        // must be LinkedHashMap, so order is preserved! Otherwise Amazon won't accept.
        @PartMap params: LinkedHashMap<String,String>,
        @Part file: MultipartBody.Part,
    ): retrofit2.Response<Unit>
}