package org.zotero.android.api.interceptors

import okhttp3.Interceptor

class DetectRedirectInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        var request = chain.request()
        var response = chain.proceed(request)

        while (isRedirect(response.code)) {
            val location = response.header("Location")
            if (!location.isNullOrBlank()) {
                break
            }
        }

        return response
    }

    private fun isRedirect(statusCode: Int): Boolean {
        return statusCode in 300..399
    }
}