package org.zotero.android.screens.allitems

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.allitems.data.ItemsError
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.quantityStringResource
import org.zotero.android.uicomponents.modal.CustomAlertDialog
import org.zotero.android.uicomponents.theme.CustomPalette

@Composable
internal fun ShowErrorOrDialog(
    itemsError: ItemsError,
    onDismissDialog: () -> Unit,
    onDeleteItems: (keys: Set<String>) -> Unit,
    onEmptyTrash: () -> Unit,
    deleteItemsFromCollection: (Set<String>) -> Unit,
) {
    when (itemsError) {
        is ItemsError.deleteConfirmationForItems -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.delete),
                description = quantityStringResource(
                    id = Plurals.items_delete_question, itemsError.itemsKeys.size
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
                description = quantityStringResource(
                    id = Plurals.items_delete_question, 2
                ),
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
        is ItemsError.showRemoveFromCollectionQuestion -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.items_remove_from_collection_title),
                description = quantityStringResource(
                    id = Plurals.items_remove_from_collection_question, itemsError.itemsKeys.size
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.no),

                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.yes),
                    onClick = {
                        deleteItemsFromCollection(itemsError.itemsKeys)
                    }
                ),
                onDismiss = onDismissDialog
            )
        }

        else -> {}
    }
}
