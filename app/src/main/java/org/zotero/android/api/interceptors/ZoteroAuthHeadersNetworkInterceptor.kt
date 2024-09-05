package org.zotero.android.api.interceptors

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response
import org.zotero.android.architecture.Defaults
import javax.inject.Inject

class ZoteroAuthHeadersNetworkInterceptor @Inject constructor(
    private val defaults: Defaults,
) : Interceptor {

    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        return when (val token = defaults.getApiToken()) {
            null -> chain.proceed(request)
            else -> runBlocking { authenticateRequest(request, token, chain) }
        }
    }

    private fun Request.setAuthorizationHeader(accessToken: String): Request =
        newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

    private fun authenticateRequest(
        request: Request,
        token: String,
        chain: Chain,
    ): Response {
        val authRequest = request.setAuthorizationHeader(token)
        return chain.proceed(authRequest)
    }
}
