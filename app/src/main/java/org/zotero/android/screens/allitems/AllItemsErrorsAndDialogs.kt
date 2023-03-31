package org.zotero.android.screens.allitems

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.allitems.data.ItemsError
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialog
import org.zotero.android.uicomponents.theme.CustomPalette

@Composable
internal fun ShowErrorOrDialog(
    itemsError: ItemsError,
    onDismissDialog: () -> Unit,
    onDeleteItems: (keys: Set<String>) -> Unit,
    onEmptyTrash: () -> Unit,
) {
    when (itemsError) {
        is ItemsError.deleteConfirmationForItems -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.delete),
                description = stringResource(
                    id = if (itemsError.itemsKeys.size == 1) {
                        Strings.delete_one_item
                    } else {
                        Strings.delete_multiple_items
                    }
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.yes),
                    textColor = CustomPalette.ErrorRed,
                    onClick = {
                        onDeleteItems(itemsError.itemsKeys)
                    }
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.no),
                ),
                onDismiss = onDismissDialog
            )
        }
        is ItemsError.deleteConfirmationForEmptyTrash -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.delete),
                description = stringResource(Strings.delete_multiple_items),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.yes),
                    textColor = CustomPalette.ErrorRed,
                    onClick = {
                        onEmptyTrash()
                    }
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.no),
                ),
                onDismiss = onDismissDialog
            )
        }

    }
}
