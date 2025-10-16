package org.zotero.android.screens.itemdetails

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.itemdetails.data.ItemDetailError
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3ActionConfig

@Composable
internal fun ItemDetailsErrorDialogs(
    itemDetailError: ItemDetailError,
    onDismissErrorDialog: () -> Unit,
    onBack: () -> Unit,
    acceptPrompt: () -> Unit,
    cancelPrompt: () -> Unit,
    acceptItemWasChangedRemotely: () -> Unit,
    deleteOrRestoreItem: (isDelete: Boolean) -> Unit,
) {
    when (itemDetailError) {
        is ItemDetailError.cantAddAttachments -> {
            when (itemDetailError.attachmentError) {
                ItemDetailError.AttachmentAddError.allFailedCreation -> TODO()
                is ItemDetailError.AttachmentAddError.couldNotMoveFromSource -> {
                    CustomAlertDialogM3(
                        title = stringResource(id = Strings.error),
                        description = stringResource(
                            id = Strings.errors_item_detail_cant_create_attachments_with_names,
                            itemDetailError.attachmentError.names.joinToString(separator = ", ")
                        ),
                        confirmButton = CustomAlertDialogM3ActionConfig(
                            text = stringResource(id = Strings.ok),
                        ),
                        onDismiss = onDismissErrorDialog
                    )
                }

                is ItemDetailError.AttachmentAddError.someFailedCreation -> {
                    CustomAlertDialogM3(
                        title = stringResource(id = Strings.error),
                        description = stringResource(
                            id = Strings.errors_item_detail_cant_create_attachments_with_names,
                            itemDetailError.attachmentError.names.joinToString(separator = ", ")
                        ),
                        confirmButton = CustomAlertDialogM3ActionConfig(
                            text = stringResource(id = Strings.ok),
                        ),
                        onDismiss = onDismissErrorDialog
                    )
                }
            }
        }

        ItemDetailError.cantCreateData -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_item_detail_cant_load_data
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(text = stringResource(id = Strings.ok)),
                onDismiss = onBack
            )
        }

        ItemDetailError.cantRemoveItem, ItemDetailError.cantRemoveParent -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_unknown
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(text = stringResource(id = Strings.ok)),
                onDismiss = onDismissErrorDialog
            )
        }

        ItemDetailError.cantSaveNote -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_item_detail_cant_save_note
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(text = stringResource(id = Strings.ok)),
                onDismiss = onDismissErrorDialog
            )
        }

        ItemDetailError.cantSaveTags -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_item_detail_cant_save_tags
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(text = stringResource(id = Strings.ok)),
                onDismiss = onDismissErrorDialog
            )
        }

        ItemDetailError.cantStoreChanges -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_item_detail_cant_save_changes
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(text = stringResource(id = Strings.ok)),
                onDismiss = onDismissErrorDialog
            )
        }

        ItemDetailError.cantTrashItem -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_item_detail_cant_trash_item
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(text = stringResource(id = Strings.ok)),
                onDismiss = onDismissErrorDialog
            )
        }

        is ItemDetailError.droppedFields -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.errors_item_detail_dropped_fields_title),
                description = droppedFieldsMessage(names = itemDetailError.fields),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = acceptPrompt
                ),
                dismissButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.cancel),
                    onClick = cancelPrompt
                ),
                onDismiss = onDismissErrorDialog
            )
        }

        is ItemDetailError.typeNotSupported -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_item_detail_unsupported_type,
                    itemDetailError.type
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(text = stringResource(id = Strings.ok)),
                onDismiss = onDismissErrorDialog
            )
        }

        is ItemDetailError.itemWasChangedRemotely -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.warning),
                description = stringResource(
                    id = Strings.item_detail_data_reloaded,
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = acceptItemWasChangedRemotely
                ),
                onDismiss = onDismissErrorDialog
            )
        }

        is ItemDetailError.askUserToDeleteOrRestoreItem -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.item_detail_deleted_title),
                description = stringResource(
                    id = Strings.item_detail_deleted_message,
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.yes),
                    onClick = { deleteOrRestoreItem(false) }),
                dismissButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.delete),
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = { deleteOrRestoreItem(true) }),
                onDismiss = onDismissErrorDialog
            )
        }
    }
}

@Composable
private fun droppedFieldsMessage(names: List<String>): String {
    val formattedNames = names.joinToString(separator = "") { "- $it\n" }
    return stringResource(id = Strings.errors_item_detail_dropped_fields_message, formattedNames)
}