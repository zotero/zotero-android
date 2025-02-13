package org.zotero.android.screens.share

import android.content.Context
import org.zotero.android.BuildConfig
import org.zotero.android.api.network.CustomResult
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.ReadGroupDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Parsing
import org.zotero.android.sync.SchemaError
import org.zotero.android.sync.SyncActionError
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.translator.data.TranslationWebViewError
import org.zotero.android.uicomponents.Strings
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareErrorProcessor @Inject constructor(
    private val context: Context,
    private val dbWrapperMain: DbWrapperMain,
) {
    fun errorMessage(error: AttachmentState.Error): String? {
        return when (error) {
            AttachmentState.Error.apiFailure -> {
                context.getString(Strings.errors_shareext_api_error)
            }

            AttachmentState.Error.cantLoadSchema -> {
                context.getString(Strings.errors_shareext_cant_load_schema)
            }

            AttachmentState.Error.cantLoadWebData -> {
                context.getString(Strings.errors_shareext_cant_load_data)
            }

            AttachmentState.Error.downloadFailed -> {
                context.getString(Strings.errors_shareext_download_failed)
            }

            AttachmentState.Error.downloadedFileNotPdf -> {
                null
            }

            AttachmentState.Error.expired -> {
                context.getString(Strings.errors_shareext_unknown)
            }

            AttachmentState.Error.fileMissing -> {
                context.getString(Strings.errors_shareext_missing_file)
            }

            AttachmentState.Error.itemsNotFound -> {
                context.getString(Strings.errors_shareext_items_not_found)
            }

            AttachmentState.Error.md5Missing -> {
                null
            }

            AttachmentState.Error.mtimeMissing -> {
                null
            }

            is AttachmentState.Error.parseError -> {
                context.getString(Strings.errors_shareext_parsing_error)
            }

            is AttachmentState.Error.quotaLimit -> {
                when (error.libraryIdentifier) {
                    is LibraryIdentifier.custom -> {
                        context.getString(Strings.errors_shareext_personal_quota_reached)
                    }

                    is LibraryIdentifier.group -> {
                        val groupId = error.libraryIdentifier.groupId
                        val group =
                            dbWrapperMain.realmDbStorage.perform(ReadGroupDbRequest(identifier = groupId))
                        val groupName = group?.name ?: "$groupId"
                        return context.getString(
                            Strings.errors_shareext_group_quota_reached,
                            groupName
                        )
                    }
                }
            }

            is AttachmentState.Error.schemaError -> {
                context.getString(Strings.errors_shareext_schema_error)
            }

            AttachmentState.Error.unknown -> {
                context.getString(Strings.errors_shareext_unknown)
            }

            AttachmentState.Error.webDavFailure -> {
                context.getString(Strings.errors_shareext_webdav_error)
            }

            AttachmentState.Error.webDavNotVerified -> {
                context.getString(Strings.errors_shareext_webdav_not_verified)
            }

            is AttachmentState.Error.webViewError -> {
                return when (error.error) {
                    TranslationWebViewError.cantFindFile -> {
                        context.getString(Strings.errors_shareext_missing_base_files)
                    }

                    TranslationWebViewError.incompatibleItem -> {
                        context.getString(Strings.errors_shareext_incompatible_item)
                    }

                    TranslationWebViewError.javascriptCallMissingResult -> {
                        context.getString(Strings.errors_shareext_javascript_failed)
                    }

                    TranslationWebViewError.noSuccessfulTranslators -> {
                        null
                    }

                    TranslationWebViewError.webExtractionMissingData -> {
                        context.getString(Strings.errors_shareext_response_missing_data)
                    }

                    TranslationWebViewError.webExtractionMissingJs -> {
                        context.getString(Strings.errors_shareext_missing_base_files)
                    }
                }
            }
        }
    }

    fun attachmentError(
        generalError: CustomResult.GeneralError,
        libraryId: LibraryIdentifier?
    ): AttachmentState.Error {
        when (generalError) {
            is CustomResult.GeneralError.CodeError -> {
                val error = generalError.throwable
                if (error is AttachmentState.Error) {
                    return error
                }
                if (error is Parsing.Error) {
                    Timber.e(error, "ExtensionViewModel: could not parse item")
                    return AttachmentState.Error.parseError(error)
                }

                if (error is SchemaError) {
                    Timber.e(error, "ExtensionViewModel: schema failed")
                    return AttachmentState.Error.schemaError(error)
                }
                if (error is TranslationWebViewError) {
                    return AttachmentState.Error.webViewError(error)
                }
                if (error is SyncActionError.authorizationFailed) {
                    if (libraryId != null) {
                        return AttachmentState.Error.quotaLimit(libraryId)
                    }
                }
            }

            is CustomResult.GeneralError.NetworkError -> {
                return networkErrorRequiresAbort(
                    error = generalError,
                    url = BuildConfig.BASE_API_URL,
                    libraryId = libraryId
                )
            }
        }
        return AttachmentState.Error.unknown
    }

    private fun networkErrorRequiresAbort(
        error: CustomResult.GeneralError.NetworkError,
        url: String?,
        libraryId: LibraryIdentifier?
    ): AttachmentState.Error {
        val defaultError = if ((url ?: "").contains(BuildConfig.BASE_API_URL)) {
            AttachmentState.Error.apiFailure
        } else {
            AttachmentState.Error.webDavFailure
        }

        val code = error.httpCode
        if (code == 413  && libraryId != null) {
            return AttachmentState.Error.quotaLimit(libraryId)
        }
        return defaultError
    }

}