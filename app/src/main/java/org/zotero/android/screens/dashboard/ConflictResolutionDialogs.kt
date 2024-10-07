package org.zotero.android.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialog
import org.zotero.android.uicomponents.theme.CustomPalette

@Composable
internal fun ConflictResolutionDialogs(
    conflictDialogData: ConflictDialogData,
    onDismissDialog: () -> Unit,
    deleteGroup: (key: Int) -> Unit,
    markGroupAsLocalOnly: (key: Int) -> Unit,
    revertGroupChanges: (key: Int) -> Unit,
    revertGroupFiles: (groupId: Int) -> Unit,
    skipGroup: (groupId: Int) -> Unit,
) {
    when (conflictDialogData) {
        is ConflictDialogData.groupRemoved -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.warning),
                description = stringResource(
                    id = Strings.errors_sync_group_removed,
                    conflictDialogData.groupName
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.remove),
                    textColor = CustomPalette.ErrorRed,
                    onClick = { deleteGroup(conflictDialogData.groupId) }
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.keep),
                    onClick = { markGroupAsLocalOnly(conflictDialogData.groupId) }
                ),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }
        is ConflictDialogData.groupMetadataWriteDenied -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.warning),
                description = stringResource(
                    id = Strings.errors_sync_metadata_write_denied,
                    conflictDialogData.groupName
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.errors_sync_revert_to_original),
                    onClick = { revertGroupChanges(conflictDialogData.groupId) }
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.errors_sync_skip_group),
                    onClick = { skipGroup(conflictDialogData.groupId) }
                ),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }
        is ConflictDialogData.groupFileWriteDenied -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.warning),
                description = stringResource(
                    id = Strings.errors_sync_file_write_denied,
                    conflictDialogData.groupName,
                    conflictDialogData.domainName
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.errors_sync_reset_group_files),
                    onClick = { revertGroupFiles(conflictDialogData.groupId) }
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.errors_sync_skip_group),
                    onClick = { skipGroup(conflictDialogData.groupId) }
                ),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }
        else -> {}
    }
}

@Composable
internal fun ChangedItemsDeletedAlert(
    conflictDialogData: ConflictDialogData.changedItemsDeletedAlert,
    deleteRemovedItemsWithLocalChanges: (key: String) -> Unit,
    restoreRemovedItemsWithLocalChanges: (key: String) -> Unit,
) {

    CustomAlertDialog(
        title = stringResource(id = Strings.warning),
        description = stringResource(
            id = Strings.sync_conflict_resolution_changed_item_deleted,
            conflictDialogData.title
        ),
        primaryAction = CustomAlertDialog.ActionConfig(
            text = stringResource(id = Strings.restore),
            onClick = { restoreRemovedItemsWithLocalChanges(conflictDialogData.key) }
        ),
        secondaryAction = CustomAlertDialog.ActionConfig(
            text = stringResource(id = Strings.delete),
            textColor = CustomPalette.ErrorRed,
            onClick = { deleteRemovedItemsWithLocalChanges(conflictDialogData.key) }
        ),
        dismissOnClickOutside = false,
        onDismiss = {}
    )

}