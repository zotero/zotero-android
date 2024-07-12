package org.zotero.android.api

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import org.zotero.android.webdav.WebDavSessionStorage
import javax.inject.Inject

class WebDavAuthNetworkInterceptor @Inject constructor(
    private val webDavSessionStorage: WebDavSessionStorage
) : Interceptor {

    override fun intercept(chain: Chain): Response {
        val request = chain.request()

        var authenticatedRequest = request.newBuilder()

        val credentials =
            Credentials.basic(webDavSessionStorage.username, webDavSessionStorage.password)
        authenticatedRequest = authenticatedRequest
            .header("Authorization", credentials)
        return chain.proceed(
            authenticatedRequest
                .build()
        )
    }
}
