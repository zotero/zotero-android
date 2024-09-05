package org.zotero.android.api.interceptors

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import org.zotero.android.architecture.logging.DeviceInfoProvider
import javax.inject.Inject

class UserAgentHeaderNetworkInterceptor @Inject constructor(
) : Interceptor {

    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        val clientInfoRequest = request.newBuilder()
            .header("User-Agent", DeviceInfoProvider.userAgentString)
            .build()
        return chain.proceed(clientInfoRequest)
    }
}
