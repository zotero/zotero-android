package org.zotero.android.api.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkHelper {
    fun <T> parseNetworkResponse(networkResponse: Response<T>): CustomResult<T> {
        if (networkResponse.isSuccessful) {
            return CustomResult.GeneralSuccess.NetworkSuccess(
                value = networkResponse.body(),
                httpCode = networkResponse.code(),
                headers = networkResponse.headers()
            )
        }
        val errorBody = networkResponse.errorBody()

        return CustomResult.GeneralError.NetworkError(
            httpUrl = networkResponse.raw().request.url,
            httpCode = networkResponse.code(),
            stringResponse = errorBody?.string()
        )
    }
    fun <T> parseNetworkException(e: Exception): CustomResult<T> {
        val isNoNetworkError = e is UnknownHostException || e is SocketTimeoutException
        return CustomResult.GeneralError.NetworkError(
            httpCode = if (isNoNetworkError) {
                CustomResult.GeneralError.NetworkError.NO_INTERNET_CONNECTION_HTTP_CODE
            } else {
                CustomResult.GeneralError.NetworkError.UNKNOWN_NETWORK_EXCEPTION_HTTP_CODE
            },
            stringResponse = e.localizedMessage,
            httpUrl = null,
        )
    }


}

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): CustomResult<T> =
    withContext(Dispatchers.IO) {
        try {
            val networkResponse = apiCall.invoke()
            val parseNetworkResponse = NetworkHelper.parseNetworkResponse(networkResponse)
            parseNetworkResponse
        } catch (e: Exception) {
            NetworkHelper.parseNetworkException(e)
        }
    }

