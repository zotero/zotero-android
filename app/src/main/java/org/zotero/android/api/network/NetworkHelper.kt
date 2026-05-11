package org.zotero.android.api.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Headers
import org.zotero.android.api.network.NetworkHelper.parseNetworkResponse
import retrofit2.Call
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

object NetworkHelper {
    fun <T> parseNetworkResponse(networkResponse: Response<T>): CustomResult<T> {
        val headers = networkResponse.headers()
        if (networkResponse.isSuccessful) {
            return CustomResult.GeneralSuccess.NetworkSuccess(
                value = networkResponse.body(),
                httpCode = networkResponse.code(),
                headers = headers
            )
        }
        val errorBody = networkResponse.errorBody()
        return CustomResult.GeneralError.NetworkError(
            httpCode = networkResponse.code(),
            stringResponse = errorBody?.string(),
            headers = headers
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
            headers = Headers.EMPTY
        )
    }


}

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): CustomResult<T> =
    withContext(Dispatchers.IO) {
        try {
            val parseNetworkResponse = retryIfNeeded(apiCall = apiCall, attempt = 0)
            parseNetworkResponse
        } catch (e: Exception) {
            NetworkHelper.parseNetworkException(e)
        }
    }

suspend fun <T> retryIfNeeded(apiCall: suspend () -> Response<T>, attempt: Int): CustomResult<T> {
    val executedApiCall = execApiCall(apiCall)
    val retryIntervalType = decideRetryIntervalType(executedApiCall)
    if (retryIntervalType == null) {
        return executedApiCall
    }
    val nextAttempt = attempt + 1
    if (nextAttempt >= RetryDelay.maxAttemptsCount) {
        return executedApiCall
    }
    val millis = retryIntervalType.millis(nextAttempt)
    delay(millis)
    return retryIfNeeded(apiCall = apiCall, attempt = nextAttempt)
}

private suspend fun <T> execApiCall(apiCall: suspend () -> Response<T>): CustomResult<T> {
    val networkResponse = apiCall.invoke()
    val parseNetworkResponse = parseNetworkResponse(networkResponse)
    return parseNetworkResponse
}

fun <T> decideRetryIntervalType(parseNetworkResponse: CustomResult<T>): RetryDelay? {
    val code = parseNetworkResponse.resultHttpCode ?: return null
    if (code == 429 || (code >= 500 && code <= 599 && code != 507)) {
        val retryHeader = parseNetworkResponse.getHeaders["Retry-After"]
            ?: parseNetworkResponse.getHeaders["Backoff"]

        if (retryHeader != null) {
            val timeAsLong = retryHeader.toLongOrNull()
            if (timeAsLong != null) {
                return RetryDelay.constant(timeAsLong * 1000L)
            }
            val timeAsDouble = retryHeader.toDoubleOrNull()
            if (timeAsDouble != null) {
                return RetryDelay.constant(timeAsDouble.toLong() * 1000L)
            }
            return RetryDelay.progressive()
        }
    }
    return null
}

fun <T> safeApiCallSync(apiCall: () -> Call<T>): CustomResult<T> =
        try {
            val networkResponse = apiCall.invoke().execute()
            val parseNetworkResponse = parseNetworkResponse(networkResponse)
            parseNetworkResponse
        } catch (e: Exception) {
            NetworkHelper.parseNetworkException(e)
        }

