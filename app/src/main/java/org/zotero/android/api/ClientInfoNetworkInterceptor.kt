package org.zotero.android.api

import android.os.Build
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import javax.inject.Inject

class ClientInfoNetworkInterceptor @Inject constructor(
    configuration: NetworkConfiguration
) : Interceptor {
    private val userAgentString =
        "zotero" +
            "android/${Build.VERSION.SDK_INT} " +
            "(${Build.MANUFACTURER} ${Build.DEVICE})"

    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        val clientInfoRequest = request.newBuilder()
            .header("X-Zotero-Client", "Android")
            .header("UserAgent", userAgentString)
            .build()
        return chain.proceed(clientInfoRequest)
    }
}
