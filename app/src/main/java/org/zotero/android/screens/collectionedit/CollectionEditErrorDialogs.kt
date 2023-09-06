package org.zotero.android.screens.collectionedit

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.collectionedit.data.CollectionEditError
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialog
import org.zotero.android.uicomponents.theme.CustomPalette

@Composable
internal fun CollectionEditErrorDialogs(
    error: CollectionEditError,
    onDismissErrorDialog: () -> Unit,
    deleteOrRestoreItem: (isDelete: Boolean) -> Unit,
) {
    when (error) {
        is CollectionEditError.askUserToDeleteOrRestoreCollection -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.item_detail_deleted_title),
                description = stringResource(
                    id = Strings.collection_was_deleted,
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.yes),
                    onClick = { deleteOrRestoreItem(false) }
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.delete),
                    textColor = CustomPalette.ErrorRed,
                    onClick = { deleteOrRestoreItem(true) }
                ),
                onDismiss = onDismissErrorDialog
            )
        }
        else -> {}
    }
}