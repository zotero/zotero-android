package org.zotero.android.translator.data

import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Parsing
import org.zotero.android.sync.SchemaError

sealed class AttachmentState {

    sealed class Error : Exception() {
        object apiFailure : Error()
        object cantLoadSchema : Error()
        object cantLoadWebData : Error()
        object downloadFailed : Error()
        object proxiedUrlsNotSupported: Error()
        object itemsNotFound : Error()
        object expired : Error()
        object unknown : Error()
        object fileMissing : Error()
        object downloadedFileNotPdf : Error()
        data class webViewError(val error: TranslationWebViewError) : Error()
        data class parseError(val error: Parsing.Error) : Error()
        data class schemaError(val error: SchemaError) : Error()
        data class quotaLimit(val libraryIdentifier: LibraryIdentifier) : Error()
        object webDavNotVerified : Error()
        object webDavFailure : Error()
        object md5Missing : Error()
        object mtimeMissing : Error()

        val isFatal: Boolean
            get() {
                return when (this) {
                    is cantLoadWebData, is cantLoadSchema, is proxiedUrlsNotSupported -> true
                    else -> false
                }
            }

        val isFatalOrQuota: Boolean
            get() {
                return when (this) {
                    is cantLoadWebData, is cantLoadSchema, is quotaLimit -> true
                    else -> false
                }
            }
    }

    object decoding : AttachmentState()
    data class translating(val message: String) : AttachmentState()
    data class downloading(val progress: Int) : AttachmentState()
    object processed : AttachmentState()
    data class failed(val e: Error) : AttachmentState()

    val error: Error?
        get() {
            when (this) {
                is failed -> return this.e
                else -> return null
            }
        }

    val translationInProgress: Boolean
        get() {
            when (this) {
                is decoding, is translating, is downloading -> {
                    return true
                }

                else -> {
                    return false
                }
            }
        }

    val isSubmittable: Boolean
        get() {
            when (this) {
                is processed -> return true
                is failed -> {
                    if (this.e.isFatal) {
                        return false
                    }
                    return when (this.e) {
                        is Error.apiFailure, is Error.quotaLimit, is Error.proxiedUrlsNotSupported -> {
                            false
                        }

                        else -> true
                    }
                }

                else -> return false
            }
        }

}