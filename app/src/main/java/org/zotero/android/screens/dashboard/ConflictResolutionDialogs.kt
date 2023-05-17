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
    keepGroupChanges: (key: Int) -> Unit,
) {
    when (conflictDialogData) {
        is ConflictDialogData.groupRemoved -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.warning),
                description = stringResource(
                    id = Strings.group_removed_message,
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
        is ConflictDialogData.groupWriteDenied -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.warning),
                description = stringResource(
                    id = Strings.group_write_denied_message,
                    conflictDialogData.groupName
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.revert_to_original),
                    onClick = { revertGroupChanges(conflictDialogData.groupId) }
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.keep_changes),
                    onClick = { keepGroupChanges(conflictDialogData.groupId) }
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
            id = Strings.changed_item_deleted,
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