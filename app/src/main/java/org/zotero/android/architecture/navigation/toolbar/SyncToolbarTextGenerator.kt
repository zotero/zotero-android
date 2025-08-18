package org.zotero.android.architecture.navigation.toolbar

import android.content.Context
import org.zotero.android.api.network.CustomResult
import org.zotero.android.architecture.navigation.toolbar.data.SyncProgress
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.ReadGroupDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncError
import org.zotero.android.sync.SyncObject
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.webdav.data.WebDavError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncToolbarTextGenerator @Inject constructor(
    private val context: Context,
    private val dbWrapperMain: DbWrapperMain,
) {

    fun syncToolbarText(progress: SyncProgress): String {
        when (progress) {
            SyncProgress.starting -> {
                return context.getString(Strings.sync_toolbar_starting)
            }

            is SyncProgress.groups -> {
                val progress = progress.progress
                if (progress != null) {
                    return context.getString(
                        Strings.sync_toolbar_groups_with_data,
                        progress.completed,
                        progress.total
                    )
                }
                return context.getString(Strings.sync_toolbar_groups)
            }

            is SyncProgress.library -> {
                return context.getString(Strings.sync_toolbar_library, progress.name)
            }

            is SyncProgress.objectS -> {
                val objectS = progress.objectS
                val libraryName = progress.libraryName
                val progress = progress.progress
                if (progress != null) {
                    return context.getString(
                        Strings.sync_toolbar_object_with_data,
                        name(objectS),
                        progress.completed,
                        progress.total,
                        libraryName
                    )
                }
                return context.getString(Strings.sync_toolbar_object, name(objectS), libraryName)
            }

            is SyncProgress.changes -> {
                val progress = progress.progress
                return context.getString(
                    Strings.sync_toolbar_writes,
                    progress.completed,
                    progress.total
                )
            }

            is SyncProgress.uploads -> {
                val progress = progress.progress
                return context.getString(
                    Strings.sync_toolbar_uploads,
                    progress.completed,
                    progress.total
                )
            }

            is SyncProgress.finished -> {
                val errors = progress.errors
                if (errors.isEmpty()) {
                    return context.getString(Strings.sync_toolbar_finished)
                }
                val issues =
                    context.resources.getQuantityString(
                        Plurals.errors_sync_toolbar_errors,
                        errors.size,
                        errors.size
                    )
                return context.getString(Strings.errors_sync_toolbar_finished_with_errors, issues)
            }

            is SyncProgress.deletions -> {
                return context.getString(Strings.sync_toolbar_deletion, progress.name)
            }

            is SyncProgress.aborted -> {
                val error = progress.error
                if (error is SyncError.Fatal.forbidden) {
                    return context.getString(Strings.errors_sync_toolbar_forbidden)
                }
                val formatArgs = syncToolbarAlertMessage(error).first
                return context.getString(
                    Strings.sync_toolbar_aborted,
                    formatArgs
                )
            }
            is SyncProgress.shouldMuteWhileOnScreen -> {
                //no op
                return ""
            }
        }
    }

    fun syncToolbarAlertMessage(
        dialogError: Exception,
    ): Pair<String, SyncError.ErrorData?> {
        val fatalError = dialogError as? SyncError.Fatal
        if (fatalError != null) {
            when (fatalError) {
                is SyncError.Fatal.cancelled -> {
                    //no-op
                }

                is SyncError.Fatal.apiError -> {
                    return context.getString(
                        Strings.errors_api,
                        fatalError.response
                    ) to fatalError.data
                }

                is SyncError.Fatal.dbError -> {
                    return context.getString(Strings.errors_db) to null
                }

                is SyncError.Fatal.allLibrariesFetchFailed -> {
                    return context.getString(Strings.errors_sync_toolbar_libraries_missing) to null
                }

                is SyncError.Fatal.uploadObjectConflict -> {
                    return context.getString(Strings.errors_sync_toolbar_conflict_retry_limit) to null
                }

                is SyncError.Fatal.groupSyncFailed -> {
                    return context.getString(Strings.errors_sync_toolbar_groups_failed) to null
                }

                is SyncError.Fatal.missingGroupPermissions, is SyncError.Fatal.permissionLoadingFailed -> {
                    return context.getString(Strings.errors_sync_toolbar_group_permissions) to null
                }

                is SyncError.Fatal.noInternetConnection -> {
                    return context.getString(Strings.errors_sync_toolbar_internet_connection) to null
                }

                is SyncError.Fatal.serviceUnavailable -> {
                    return context.getString(Strings.errors_sync_toolbar_unavailable) to null
                }

                is SyncError.Fatal.forbidden -> {
                    return context.getString(Strings.errors_sync_toolbar_forbidden_message) to null
                }

                is SyncError.Fatal.cantSubmitAttachmentItem -> {
                    return context.getString(Strings.errors_db) to fatalError.data
                }

            }
        }

        val nonFatalError = dialogError as? SyncError.NonFatal

        if (nonFatalError != null) {
            when (nonFatalError) {
                is SyncError.NonFatal.schema -> {
                    return context.getString(Strings.errors_schema) to null
                }

                is SyncError.NonFatal.parsing -> {
                    return context.getString(Strings.errors_parsing) to null
                }

                is SyncError.NonFatal.apiError -> {
                    return context.getString(
                        Strings.errors_api,
                        nonFatalError.response
                    ) to nonFatalError.data
                }

                is SyncError.NonFatal.versionMismatch -> {
                    return context.getString(Strings.errors_versionMismatch) to null
                }

                is SyncError.NonFatal.unknown -> {
                    return if (nonFatalError.messageS.isEmpty()) {
                        context.getString(Strings.errors_unknown) to nonFatalError.data
                    } else {
                        nonFatalError.messageS to nonFatalError.data
                    }
                }

                is SyncError.NonFatal.attachmentMissing -> {
                    return context.getString(
                        Strings.errors_sync_toolbar_attachment_missing,
                        "${nonFatalError.title} (${nonFatalError.key})"
                    ) to SyncError.ErrorData(
                        itemKeys = listOf(nonFatalError.key),
                        libraryId = nonFatalError.libraryId
                    )
                }

                is SyncError.NonFatal.quotaLimit -> {
                    when (nonFatalError.libraryId) {
                        is LibraryIdentifier.custom -> {
                            return context.getString(
                                Strings.errors_sync_toolbar_personal_quota_reached
                            ) to null
                        }

                        is LibraryIdentifier.group -> {
                            val groupId = nonFatalError.libraryId.groupId
                            val groupName = getGroupNameById(groupId)
                            return context.getString(
                                Strings.errors_sync_toolbar_group_quota_reached, groupName
                            ) to null
                        }
                    }
                }

                is SyncError.NonFatal.insufficientSpace -> {
                    return context.getString(Strings.errors_sync_toolbar_insufficient_space) to null
                }

                is SyncError.NonFatal.webDavDeletionFailed -> {
                    return context.getString(Strings.errors_sync_toolbar_webdav_error) to null
                }

                is SyncError.NonFatal.webDavDeletion -> {
                    return context.resources.getQuantityString(
                        Plurals.errors_sync_toolbar_webdav_error2,
                        nonFatalError.count,
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
                    return context.getString(Strings.errors_sync_toolbar_conflict_retry_limit) to SyncError.ErrorData(
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
                                return context.getString(
                                    Strings.errors_sync_toolbar_webdav_request_failed,
                                    statusCode.resultHttpCode ?: -1,
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
                            return context.getString(
                                Strings.errors_sync_toolbar_webdav_item_prop,
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
                    return context.getString(
                        Strings.errors_sync_toolbar_webdav_item_prop,
                        error.message
                    ) to null
                }
            }
        }
        return "" to null
    }

    private fun name(objectS: SyncObject): String {
        return when (objectS) {
            SyncObject.collection -> {
                context.getString(Strings.sync_toolbar_object_collections)
            }

            SyncObject.item, SyncObject.trash -> {
                context.getString(Strings.sync_toolbar_object_items)
            }

            SyncObject.search -> {
                context.getString(Strings.sync_toolbar_object_searches)
            }

            SyncObject.settings -> ""
        }
    }

    private fun getGroupNameById(groupId: Int): String {
        val group =
            dbWrapperMain.realmDbStorage.perform(request = ReadGroupDbRequest(identifier = groupId))
        val groupName = group.name ?: "$groupId"
        return groupName
    }
}

