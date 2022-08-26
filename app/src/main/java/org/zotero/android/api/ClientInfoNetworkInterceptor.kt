package org.zotero.android.api

import android.os.Build
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import javax.inject.Inject

class ClientInfoNetworkInterceptor @Inject constructor(
    configuration: ApiConfiguration
) : Interceptor {
    private val clientVersion = configuration.appVersion
    private val userAgentString =
        "zotero-$clientVersion " +
            "android/${Build.VERSION.SDK_INT} " +
            "(${Build.MANUFACTURER} ${Build.DEVICE})"

    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        val clientInfoRequest = request.newBuilder()
            .header("X-Zotero-Client", "Android")
            .header("X-Zotero-Client-Version", clientVersion)
            .header("UserAgent", userAgentString)
            .build()
        return chain.proceed(clientInfoRequest)
    }
}
