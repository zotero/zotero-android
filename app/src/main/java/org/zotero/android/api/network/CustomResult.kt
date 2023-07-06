package org.zotero.android.api.network

import okhttp3.Headers

sealed class CustomResult<out T> {
    sealed class GeneralError : CustomResult<Nothing>() {
        data class NetworkError(val httpCode: Int, val stringResponse: String?) : GeneralError() {
            fun isUnchanged(): Boolean {
                return httpCode == 304
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