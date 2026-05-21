package org.zotero.android.api.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Headers
import org.zotero.android.api.network.NetworkHelper.parseNetworkException
import org.zotero.android.api.network.NetworkHelper.parseNetworkResponse
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Locale
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
            val parseNetworkResponse = execCallAndRetryIfNeeded(apiCall = apiCall, attempt = 0)
            parseNetworkResponse
        } catch (e: Exception) {
            parseNetworkException(e)
        }
    }

suspend fun <T> safeApiCallForZoteroSync(apiCall: suspend () -> Response<T>): CustomResult<T> =
    withContext(Dispatchers.IO) {
        try {
            val parsedNetworkResponse = execCallAndRetryIfNeeded(apiCall = apiCall, attempt = 0)
            delayForBackoffIfNeeded(parsedNetworkResponse)
            parsedNetworkResponse
        } catch (e: Exception) {
            parseNetworkException(e)
        }
    }

private suspend fun delayForBackoffIfNeeded(parsedNetworkResponse: CustomResult<*>) {
    val backoffHeader = parsedNetworkResponse.getHeaders["Backoff"]
    if (backoffHeader != null) {
        val delay = backoffHeader.toLongOrNull()
        if (delay != null) {
            delay(delay * 1000L)
        }
    }
}

private suspend fun <T> execCallAndRetryIfNeeded(apiCall: suspend () -> Response<T>, attempt: Int): CustomResult<T> {
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
    return execCallAndRetryIfNeeded(apiCall = apiCall, attempt = nextAttempt)
}

private suspend fun <T> execApiCall(apiCall: suspend () -> Response<T>): CustomResult<T> {
    val networkResponse = apiCall.invoke()
    val parseNetworkResponse = parseNetworkResponse(networkResponse)
    return parseNetworkResponse
}

fun <T> decideRetryIntervalType(parseNetworkResponse: CustomResult<T>): RetryDelay? {
    val code = parseNetworkResponse.resultHttpCode ?: return null
    if (code == 429 || (code in 500..599 && code != 507)) {
        val retryAfterHeader = parseNetworkResponse.getHeaders["Retry-After"]
        if (retryAfterHeader != null) {
            val parsedDelay = parseRetryAfterHeader(retryAfterHeader)
            if (parsedDelay != null) {
                return RetryDelay.constant(parsedDelay)
            }
        }
        return RetryDelay.progressive()
    }
    return null
}

private val rfc1123DateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)

private fun parseRetryAfterHeader(headerValue: String): Long? {
    headerValue.toLongOrNull()?.let { seconds ->
        return seconds * 1000L
    }
    try {
        val parsedDateMillis = rfc1123DateFormat.parse(headerValue).time
        val nowMillis = System.currentTimeMillis()
        val delayMillis = parsedDateMillis - nowMillis

        return if (delayMillis > 0) delayMillis else null
    } catch (e: Exception) {
        Timber.e(e, "Not a valid http-date: $headerValue")
    }
    return null
}


fun <T> webDavDeleteSafeApiCall(apiCall: () -> Call<T>): CustomResult<T> =
        try {
            val networkResponse = apiCall.invoke().execute()
            val parseNetworkResponse = parseNetworkResponse(networkResponse)
            parseNetworkResponse
        } catch (e: Exception) {
            parseNetworkException(e)
        }

