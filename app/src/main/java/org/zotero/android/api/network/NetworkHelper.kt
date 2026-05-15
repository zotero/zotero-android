package org.zotero.android.api.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Headers
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
            } else if (isNoCertificateError) {
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
    val retryDelay = decideRetryDelay(executedApiCall)

    if (retryDelay == null) {
        return executedApiCall
    }

    val nextAttempt = attempt + 1
    if (nextAttempt >= RetryDelay.maxAttemptsCount) {
        return executedApiCall
    }

    val millis = retryDelay.millis(nextAttempt)
    delay(millis)
    return retryIfNeeded(apiCall = apiCall, attempt = nextAttempt)
}

private suspend fun <T> execApiCall(apiCall: suspend () -> Response<T>): CustomResult<T> {
    val networkResponse = apiCall.invoke()
    return NetworkHelper.parseNetworkResponse(networkResponse)
}

fun <T> decideRetryDelay(result: CustomResult<T>): RetryDelay? {
    val httpCode = result.resultHttpCode ?: return null

    val isRateLimited = httpCode == 429
    val isServerError = httpCode in 500..599 && httpCode != 507
    val shouldBackoff = isRateLimited || isServerError

    if (!shouldBackoff) {
        val backoffHeader = result.getHeaders["Backoff"]
        if (result is CustomResult.GeneralSuccess && backoffHeader != null) {
            return parseBackoffHeader(backoffHeader)
        }
        return null
    }

    val retryAfterHeader = result.getHeaders["Retry-After"]
    if (retryAfterHeader != null) {
        val parsedDelay = parseRetryAfterHeader(retryAfterHeader)
        if (parsedDelay != null) {
            return RetryDelay.constant(parsedDelay)
        }
    }

    // No Retry-After header, use progressive backoff
    return RetryDelay.progressive()
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
private fun parseBackoffHeader(headerValue: String?): RetryDelay? {
    if (headerValue == null) return null

    val seconds = headerValue.toLongOrNull()
    return if (seconds != null && seconds > 0) {
        RetryDelay.constant(seconds * 1000L)
    } else {
        null
    }
}

fun <T> safeApiCallSync(apiCall: () -> Call<T>): CustomResult<T> =
    try {
        val networkResponse = apiCall.invoke().execute()
        NetworkHelper.parseNetworkResponse(networkResponse)
    } catch (e: Exception) {
        NetworkHelper.parseNetworkException(e)
    }