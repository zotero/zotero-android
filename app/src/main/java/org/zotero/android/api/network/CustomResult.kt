package org.zotero.android.api.network

import okhttp3.Headers
import org.zotero.android.sync.PreconditionErrorType

sealed class CustomResult<out T> {
    sealed class GeneralError : CustomResult<Nothing>() {
        data class NetworkError(val httpCode: Int, val stringResponse: String?) : GeneralError() {
            fun isUnchanged(): Boolean {
                return httpCode == 304
            }
        }

        data class CodeError(val throwable: Throwable) : GeneralError() {

        }

        val preconditionError: PreconditionErrorType? get() {
            val codeError = this as? CodeError
            if (codeError != null) {
                if (codeError.throwable as? PreconditionErrorType != null) {
                    return codeError.throwable
                }
            }
            val networkError = this as? NetworkError
            if (networkError != null && networkError.httpCode == 412) {
                return PreconditionErrorType.libraryConflict
            }
            return null
        }
    }

    open class GeneralSuccess<out T>(open val value: T) : CustomResult<T>() {
        class NetworkSuccess<out T>(
            override val value: T,
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