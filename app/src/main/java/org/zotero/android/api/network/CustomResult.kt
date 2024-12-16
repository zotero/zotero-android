package org.zotero.android.api.network

import okhttp3.Headers
import org.zotero.android.api.network.CustomResult.GeneralSuccess.NetworkSuccess

sealed class CustomResult<out T> {
    sealed class GeneralError : CustomResult<Nothing>() {
        open class NetworkError(
            open val httpCode: Int,
            open val stringResponse: String?,
        ) : GeneralError() {
            companion object {
                const val NO_INTERNET_CONNECTION_HTTP_CODE = -1
                const val NO_HTTPS_CERTIFICATE_FOUND = -2
                const val UNKNOWN_NETWORK_EXCEPTION_HTTP_CODE = -999
            }


            fun isUnchanged(): Boolean {
                return httpCode == 304
            }
            fun isNoNetworkError(): Boolean {
                return httpCode == NO_INTERNET_CONNECTION_HTTP_CODE
            }
            fun isNoCertificateFound(): Boolean {
                return httpCode == NO_HTTPS_CERTIFICATE_FOUND
            }
            fun isNotFound(): Boolean {
                return httpCode == 404
            }
        }

        data class UnacceptableStatusCode(
            override val httpCode: Int,
            override val stringResponse: String?,
        ) : NetworkError(httpCode, stringResponse)

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

    val resultHttpCode: Int? get() {
        return when (this) {
            is NetworkSuccess -> {
                this.httpCode
            }

            is GeneralError.NetworkError -> {
                this.httpCode
            }

            else -> null
        }
    }
}