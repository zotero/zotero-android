package org.zotero.android.webdav.data

import android.content.Context
import org.zotero.android.ZoteroApplication
import org.zotero.android.api.network.CustomResult
import org.zotero.android.uicomponents.Strings

sealed class WebDavError {
    sealed class Verification : Exception()  {
        object noUrl: Verification()
        object noUsername: Verification()
        object noPassword: Verification()
        object invalidUrl: Verification()
        object notDav: Verification()
        object parentDirNotFound: Verification()
        data class zoteroDirNotFound(val url: String): Verification()
        object nonExistentFileNotMissing: Verification()
        object fileMissingAfterUpload: Verification()
        object localHttpWebdavHostNotAllowed: Verification()

        override val message: String get() {
            val context = ZoteroApplication.instance
            return when(this) {
                fileMissingAfterUpload -> {
                    context.getString(Strings.errors_settings_webdav_file_missing_after_upload)
                }
                invalidUrl -> {
                    context.getString(Strings.errors_settings_webdav_invalid_url)
                }
                noPassword -> {
                    context.getString(Strings.errors_settings_webdav_no_password)
                }
                noUrl -> {
                    context.getString(Strings.errors_settings_webdav_no_url)
                }
                noUsername -> {
                    context.getString(Strings.errors_settings_webdav_no_username)
                }
                nonExistentFileNotMissing -> {
                    context.getString(Strings.errors_settings_webdav_non_existent_file_not_missing)
                }
                notDav -> {
                    context.getString(Strings.errors_settings_webdav_not_dav)
                }
                parentDirNotFound -> {
                    context.getString(Strings.errors_settings_webdav_parent_dir_not_found)
                }
                is zoteroDirNotFound -> {
                    context.getString(Strings.errors_settings_webdav_zotero_dir_not_found)
                }
                localHttpWebdavHostNotAllowed -> {
                    context.getString(Strings.errors_settings_webdav_local_http_host_not_allowed)
                }
            }
        }
    }

    sealed class Download : Exception() {
        data class itemPropInvalid(val str: String) : Download()
        object notChanged : Download()
    }

    sealed class Upload : Exception() {
        object cantCreatePropData : Upload()

        data class apiError(val error: CustomResult.GeneralError.NetworkError, val httpMethod: String?) : Upload()
    }

    companion object {
        fun message(error: CustomResult.GeneralError): String {
            val context = ZoteroApplication.instance

            val verificationError = (error as? CustomResult.GeneralError.CodeError)?.throwable as? WebDavError.Verification
            if (verificationError != null) {
                return verificationError.message
            }

            val networkError = error as? CustomResult.GeneralError.NetworkError
            if (networkError != null) {
                val message = errorMessage(context, networkError)
                if (message != null) {
                    return message
                }
            }

            return when (error) {
                is CustomResult.GeneralError.CodeError -> {
                    error.throwable.localizedMessage
                }

                is CustomResult.GeneralError.NetworkError -> {
                    error.stringResponse ?: "Unknown network error"
                }
            }
        }

        private fun errorMessage(
            context: Context,
            error: CustomResult.GeneralError.NetworkError
        ): String? {
                when (error.httpCode) {
                    401 -> {
                        return context.getString(Strings.errors_settings_webdav_unauthorized)
                    }
                    400 -> {
                        return error.stringResponse
                    }
                    403 -> {
                        return context.getString(Strings.errors_settings_webdav_forbidden)
                    }

                }
            if (error.isNoNetworkError()) {
                return context.getString(Strings.errors_settings_webdav_internet_connection)
            }
            if (error.isNoCertificateFound()) {
                return context.getString(Strings.errors_settings_webdav_no_https_certificate)
            }

            return context.getString(Strings.errors_settings_webdav_host_not_found)
        }

    }

}
