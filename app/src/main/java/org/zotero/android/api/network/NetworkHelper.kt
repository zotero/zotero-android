package org.zotero.android.api.network

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zotero.android.api.network.NetworkHelper.getCurrentConnectionType
import org.zotero.android.framework.ZoteroApplication
import retrofit2.HttpException
import java.io.IOException

object NetworkHelper {
    fun getCurrentConnectionType(): ConnectionType {
        val context: Context = ZoteroApplication.instance.applicationContext
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        val isConnected = activeNetwork != null &&
                activeNetwork.isConnected
        if (!isConnected) {
            return ConnectionType.none
        }
        val isWiFi = activeNetwork!!.type == ConnectivityManager.TYPE_WIFI
        return if (isWiFi) {
            ConnectionType.wifi
        } else {
            ConnectionType.cellular
        }
    }

}

suspend fun <T> safeApiCall(retry: Int = 0, apiCall: suspend () -> T):
        NetworkResultWrapper<T> = withContext(Dispatchers.IO) {
    try {
        val value = apiCall.invoke()
        NetworkResultWrapper.Success(value)
    } catch (throwable: Throwable) {
        val customNetworkError = convertToCustomNetworkError(throwable)
        if (customNetworkError.shouldRetry && retry > 0) {
            safeApiCall(retry - 1, apiCall)
        } else {
            customNetworkError
        }
    }
}

private fun convertToCustomNetworkError(throwable: Throwable): NetworkResultWrapper.NetworkError {
    if (getCurrentConnectionType() == ConnectionType.none) {
        return NetworkResultWrapper.NetworkError(
            CustomNetworkError(throwable.localizedMessage!!, -1), true
        )
    }
    if (throwable is HttpException) {
        val response = throwable.response()
        val code = throwable.code()

        if (code < 200 || code >= 300) {
            var errorMessage = ""
            if (response?.errorBody() != null) {
                errorMessage = response.errorBody()!!.string()
            }
            return NetworkResultWrapper.NetworkError(
                CustomNetworkError(errorMessage, code), false
            )
        }
    }

    // A network error happened
    if (throwable is IOException) {
        return NetworkResultWrapper.NetworkError(
            CustomNetworkError(throwable.getLocalizedMessage()!!, -1), true
        )
    }

    // We don't know what happened. We need to simply convert to an unknown error
    return NetworkResultWrapper.NetworkError(
        CustomNetworkError("Invalid url response", -1), false
    )
}