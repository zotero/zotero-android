package org.zotero.android.architecture.navigation.toolbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.architecture.navigation.toolbar.data.SyncProgress
import org.zotero.android.sync.SyncError
import org.zotero.android.sync.SyncObject
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.quantityStringResource

@Composable
fun syncToolbarText(progress: SyncProgress, viewModel: SyncToolbarViewModel): String {
    when (progress) {
        SyncProgress.starting -> {
            return stringResource(id = Strings.sync_toolbar_starting)
        }

        is SyncProgress.groups -> {
            val progress = progress.progress
            if (progress != null) {
                return stringResource(
                    id = Strings.sync_toolbar_groups_with_data,
                    progress.completed,
                    progress.total
                )
            }
            return stringResource(id = Strings.sync_toolbar_groups)
        }

        is SyncProgress.library -> {
            return stringResource(id = Strings.sync_toolbar_library, progress.name)
        }

        is SyncProgress.objectS -> {
            val objectS = progress.objectS
            val libraryName = progress.libraryName
            val progress = progress.progress
            if (progress != null) {
                return stringResource(
                    id = Strings.sync_toolbar_object_with_data,
                    name(objectS),
                    progress.completed,
                    progress.total,
                    libraryName
                )
            }
            return stringResource(id = Strings.sync_toolbar_object, name(objectS), libraryName)
        }

        is SyncProgress.changes -> {
            val progress = progress.progress
            return stringResource(
                id = Strings.sync_toolbar_writes,
                progress.completed,
                progress.total
            )
        }

        is SyncProgress.uploads -> {
            val progress = progress.progress
            return stringResource(
                id = Strings.sync_toolbar_uploads,
                progress.completed,
                progress.total
            )
        }

        is SyncProgress.finished -> {
            val errors = progress.errors
            if (errors.isEmpty()) {
                return stringResource(id = Strings.sync_toolbar_finished)
            }
            val issues =
                quantityStringResource(id = Plurals.errors_sync_toolbar_errors, errors.size)
            return stringResource(id = Strings.errors_sync_toolbar_finished_with_errors, issues)
        }

        is SyncProgress.deletions -> {
            return stringResource(id = Strings.sync_toolbar_deletion, progress.name)
        }

        is SyncProgress.aborted -> {
            val error = progress.error
            if (error is SyncError.Fatal.forbidden) {
                return stringResource(id = Strings.errors_sync_toolbar_forbidden)
            }
            val formatArgs = syncToolbarAlertMessage(error, viewModel = viewModel).first
            return stringResource(
                id = Strings.sync_toolbar_aborted,
                formatArgs
            )
        }
    }
}

@Composable
private fun name(objectS: SyncObject): String {
    when (objectS) {
        SyncObject.collection -> {
            return stringResource(id = Strings.sync_toolbar_object_collections)
        }

        SyncObject.item, SyncObject.trash -> {
            return stringResource(id = Strings.sync_toolbar_object_items)
        }

        SyncObject.search -> {
            return stringResource(id = Strings.sync_toolbar_object_searches)
        }

        SyncObject.settings -> return ""
    }
}