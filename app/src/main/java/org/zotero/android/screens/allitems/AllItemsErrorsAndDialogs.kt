package org.zotero.android.screens.allitems

import androidx.compose.runtime.Composable
import org.zotero.android.screens.allitems.data.ItemsError
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeQuantityStringResource
import org.zotero.android.uicomponents.foundation.safeStringResource
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3ActionConfig
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
            CustomAlertDialogM3(
                title = safeStringResource(id = Strings.delete),
                description = safeQuantityStringResource(
                    id = Plurals.items_delete_question, itemsError.itemsKeys.size
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = safeStringResource(id = Strings.yes),
                    textColor = CustomPalette.ErrorRed,
                    onClick = {
                        onDeleteItems(itemsError.itemsKeys)
                    }
                ),
                dismissButton = CustomAlertDialogM3ActionConfig(
                    text = safeStringResource(id = Strings.no),
                ),
                onDismiss = onDismissDialog
            )
        }
        is ItemsError.deleteConfirmationForEmptyTrash -> {
            CustomAlertDialogM3(
                title = safeStringResource(id = Strings.delete),
                description = safeQuantityStringResource(
                    id = Plurals.items_delete_question, 2
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = safeStringResource(id = Strings.yes),
                    textColor = CustomPalette.ErrorRed,
                    onClick = {
                        onEmptyTrash()
                    }
                ),
                dismissButton = CustomAlertDialogM3ActionConfig(
                    text = safeStringResource(id = Strings.no),
                ),
                onDismiss = onDismissDialog
            )
        }
        is ItemsError.showRemoveFromCollectionQuestion -> {
            CustomAlertDialogM3(
                title = safeStringResource(id = Strings.items_remove_from_collection_title),
                description = safeQuantityStringResource(
                    id = Plurals.items_remove_from_collection_question, itemsError.itemsKeys.size
                ),
                dismissButton = CustomAlertDialogM3ActionConfig(
                    text = safeStringResource(id = Strings.no),

                ),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = safeStringResource(id = Strings.yes),
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
