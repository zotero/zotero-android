package org.zotero.android.api.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

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
            httpCode = networkResponse.code(),
            stringResponse = errorBody?.string()
        )
    }
    fun <T> parseNetworkException(e: Exception): CustomResult<T> {
        val isNoNetworkError = e is UnknownHostException || e is SocketTimeoutException
        val isNoCertificateError = e is SSLHandshakeException
        return CustomResult.GeneralError.NetworkError(
            httpCode = if (isNoNetworkError) {
                CustomResult.GeneralError.NetworkError.NO_INTERNET_CONNECTION_HTTP_CODE
            } else if (isNoCertificateError){
                CustomResult.GeneralError.NetworkError.NO_HTTPS_CERTIFICATE_FOUND
            } else {
                CustomResult.GeneralError.NetworkError.UNKNOWN_NETWORK_EXCEPTION_HTTP_CODE
            },
            stringResponse = e.localizedMessage,
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

fun <T> safeApiCallSync(apiCall: () -> Call<T>): CustomResult<T> =
        try {
            val networkResponse = apiCall.invoke().execute()
            val parseNetworkResponse = NetworkHelper.parseNetworkResponse(networkResponse)
            parseNetworkResponse
        } catch (e: Exception) {
            NetworkHelper.parseNetworkException(e)
        }

