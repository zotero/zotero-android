package org.zotero.android.api.network

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zotero.android.framework.ZoteroApplication
import retrofit2.Response

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


    fun <T> parseNetworkResponse(networkResponse: Response<T>): CustomResult<T> {
        if (networkResponse.isSuccessful) {
            return CustomResult.GeneralSuccess.NetworkSuccess(
                value = networkResponse.body()!!,
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

}

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): CustomResult<T> =
    withContext(Dispatchers.IO) {
        val networkResponse = apiCall.invoke()
        val parseNetworkResponse = NetworkHelper.parseNetworkResponse(networkResponse)
        parseNetworkResponse
    }

