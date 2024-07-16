package org.zotero.android.architecture.navigation.toolbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.api.network.CustomResult
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncError
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.quantityStringResource
import org.zotero.android.webdav.data.WebDavError

@Composable
fun syncToolbarAlertMessage(
    dialogError: Exception,
    viewModel: SyncToolbarViewModel
): Pair<String, SyncError.ErrorData?> {
    val fatalError = dialogError as? SyncError.Fatal
    if (fatalError != null) {
        when (fatalError) {
            is SyncError.Fatal.cancelled -> {
                //no-op
            }

            is SyncError.Fatal.apiError -> {
                return stringResource(
                    id = Strings.errors_api,
                    fatalError.response
                ) to fatalError.data
            }

            is SyncError.Fatal.dbError -> {
                return stringResource(id = Strings.errors_db) to null
            }

            is SyncError.Fatal.allLibrariesFetchFailed -> {
                return stringResource(id = Strings.errors_sync_toolbar_libraries_missing) to null
            }

            is SyncError.Fatal.uploadObjectConflict -> {
                return stringResource(id = Strings.errors_sync_toolbar_conflict_retry_limit) to null
            }

            is SyncError.Fatal.groupSyncFailed -> {
                return stringResource(id = Strings.errors_sync_toolbar_groups_failed) to null
            }

            is SyncError.Fatal.missingGroupPermissions, is SyncError.Fatal.permissionLoadingFailed -> {
                return stringResource(id = Strings.errors_sync_toolbar_group_permissions) to null
            }

            is SyncError.Fatal.noInternetConnection -> {
                return stringResource(id = Strings.errors_sync_toolbar_internet_connection) to null
            }

            is SyncError.Fatal.serviceUnavailable -> {
                return stringResource(id = Strings.errors_sync_toolbar_unavailable) to null
            }

            is SyncError.Fatal.forbidden -> {
                return stringResource(id = Strings.errors_sync_toolbar_forbidden_message) to null
            }

            is SyncError.Fatal.cantSubmitAttachmentItem -> {
                return stringResource(id = Strings.errors_db) to fatalError.data
            }

        }
    }

    val nonFatalError = dialogError as? SyncError.NonFatal

    if (nonFatalError != null) {
        when (nonFatalError) {
            is SyncError.NonFatal.schema -> {
                return stringResource(id = Strings.errors_schema) to null
            }

            is SyncError.NonFatal.parsing -> {
                return stringResource(id = Strings.errors_parsing) to null
            }

            is SyncError.NonFatal.apiError -> {
                return stringResource(
                    id = Strings.errors_api,
                    nonFatalError.response
                ) to nonFatalError.data
            }

            is SyncError.NonFatal.versionMismatch -> {
                return stringResource(id = Strings.errors_versionMismatch) to null
            }

            is SyncError.NonFatal.unknown -> {
                return if (nonFatalError.messageS.isEmpty()) {
                    stringResource(id = Strings.errors_unknown) to nonFatalError.data
                } else {
                    nonFatalError.messageS to nonFatalError.data
                }
            }

            is SyncError.NonFatal.attachmentMissing -> {
                return stringResource(
                    id = Strings.errors_sync_toolbar_attachment_missing,
                    "${nonFatalError.title} (${nonFatalError.key})"
                ) to SyncError.ErrorData(
                    itemKeys = listOf(nonFatalError.key),
                    libraryId = nonFatalError.libraryId
                )
            }

            is SyncError.NonFatal.quotaLimit -> {
                when (nonFatalError.libraryId) {
                    is LibraryIdentifier.custom -> {
                        return stringResource(
                            id = Strings.errors_sync_toolbar_personal_quota_reached
                        ) to null
                    }

                    is LibraryIdentifier.group -> {
                        val groupId = nonFatalError.libraryId.groupId
                        val groupName = viewModel.getGroupNameById(groupId)
                        return stringResource(
                            id = Strings.errors_sync_toolbar_group_quota_reached, groupName
                        ) to null
                    }
                }
            }

            is SyncError.NonFatal.insufficientSpace -> {
                return stringResource(id = Strings.errors_sync_toolbar_insufficient_space) to null
            }

            is SyncError.NonFatal.webDavDeletionFailed -> {
                return stringResource(id = Strings.errors_sync_toolbar_webdav_error) to null
            }

            is SyncError.NonFatal.webDavDeletion -> {
                return quantityStringResource(
                    id = Plurals.errors_sync_toolbar_webdav_error2,
                    nonFatalError.count
                ) to null
            }

            is SyncError.NonFatal.annotationDidSplit -> {
                val string = nonFatalError.messageS
                val keys = nonFatalError.keys
                val libraryId = nonFatalError.libraryId
                return (string to SyncError.ErrorData(
                    itemKeys = keys.toList(),
                    libraryId = libraryId
                ))
            }

            is SyncError.NonFatal.unchanged -> {
                //no-op
            }

            is SyncError.NonFatal.preconditionFailed -> {
                return stringResource(id = Strings.errors_sync_toolbar_conflict_retry_limit) to SyncError.ErrorData(
                    itemKeys = null,
                    libraryId = nonFatalError.libraryId
                )
            }

            is SyncError.NonFatal.webDavUpload -> {
                val error = nonFatalError.error
                when (error) {
                    WebDavError.Upload.cantCreatePropData -> {
                        //no-op
                    }

                    is WebDavError.Upload.apiError -> {
                        val apiError = error.error
                        val httpMethod = error.httpMethod
                        val statusCode =
                            apiError as? CustomResult.GeneralError.UnacceptableStatusCode
                        if (statusCode != null) {
                            return stringResource(
                                id = Strings.errors_sync_toolbar_webdav_request_failed,
                                statusCode,
                                httpMethod ?: "Unknown"
                            ) to null
                        }
                        return WebDavError.message(apiError) to null
                    }
                }
            }
            is SyncError.NonFatal.webDavDownload -> {
                val error = nonFatalError.error
                when (error) {
                    is WebDavError.Download.itemPropInvalid -> {
                        return stringResource(
                            id = Strings.errors_sync_toolbar_webdav_item_prop,
                            error.str
                        ) to null
                    }
                    WebDavError.Download.notChanged -> {
                        //no-op
                    }
                }
            }
            is SyncError.NonFatal.webDavVerification -> {
                val error = nonFatalError.error
                return stringResource(
                    id = Strings.errors_sync_toolbar_webdav_item_prop,
                    error.message
                ) to null
            }
        }
    }
    return "" to null
}
