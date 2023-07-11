package org.zotero.android.api.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

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

}

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): CustomResult<T> =
    withContext(Dispatchers.IO) {
        try {
            val networkResponse = apiCall.invoke()
            val parseNetworkResponse = NetworkHelper.parseNetworkResponse(networkResponse)
            parseNetworkResponse
        } catch (e: Exception) {
            CustomResult.GeneralError.CodeError(e)
        }
    }

