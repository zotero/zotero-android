package org.zotero.android.api.network

import okhttp3.Headers

sealed class CustomResult<out T> {
    sealed class GeneralError : CustomResult<Nothing>() {
        data class NetworkError(val httpCode: Int, val stringResponse: String?) : GeneralError() {
            companion object {
                const val NO_INTERNET_CONNECTION_HTTP_CODE = -1
                const val UNKNOWN_NETWORK_EXCEPTION_HTTP_CODE = -999
            }


            fun isUnchanged(): Boolean {
                return httpCode == 304
            }
            fun isNoNetworkError(): Boolean {
                return httpCode == NO_INTERNET_CONNECTION_HTTP_CODE
            }
        }

        data class CodeError(val throwable: Throwable) : GeneralError() {

        }
    }

    open class GeneralSuccess<out T>(open val value: T?) : CustomResult<T>() {
        class NetworkSuccess<out T>(
            override val value: T?,
            val headers: Headers,
            val httpCode: Int
        ) : GeneralSuccess<T>(value) {
            val lastModifiedVersion: Int
                get() {
                    return headers["last-modified-version"]?.toInt() ?: 0
                }
        }
    }
}