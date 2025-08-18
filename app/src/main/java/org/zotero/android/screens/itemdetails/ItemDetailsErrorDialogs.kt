package org.zotero.android.screens.itemdetails

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.itemdetails.data.ItemDetailError
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialog
import org.zotero.android.uicomponents.theme.CustomPalette

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
                    CustomAlertDialog(
                        title = stringResource(id = Strings.error),
                        description = stringResource(
                            id = Strings.errors_item_detail_cant_create_attachments_with_names,
                            itemDetailError.attachmentError.names.joinToString(separator = ", ")
                        ),
                        primaryAction = CustomAlertDialog.ActionConfig(
                            text = stringResource(id = Strings.ok),
                            onClick = onDismissErrorDialog
                        ),
                        onDismiss = onDismissErrorDialog
                    )
                }
                is ItemDetailError.AttachmentAddError.someFailedCreation -> {
                    CustomAlertDialog(
                        title = stringResource(id = Strings.error),
                        description = stringResource(
                            id = Strings.errors_item_detail_cant_create_attachments_with_names,
                            itemDetailError.attachmentError.names.joinToString(separator = ", ")
                        ),
                        primaryAction = CustomAlertDialog.ActionConfig(
                            text = stringResource(id = Strings.ok),
                            onClick = onDismissErrorDialog
                        ),
                        onDismiss = onDismissErrorDialog
                    )
                }
            }
        }
        ItemDetailError.cantCreateData -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_item_detail_cant_load_data
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = onBack
                ),
                onDismiss = onBack
            )
        }
        ItemDetailError.cantRemoveItem, ItemDetailError.cantRemoveParent -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_unknown
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = onDismissErrorDialog
                ),
                onDismiss = onDismissErrorDialog
            )
        }
        ItemDetailError.cantSaveNote -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_item_detail_cant_save_note
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = onDismissErrorDialog
                ),
                onDismiss = onDismissErrorDialog
            )
        }
        ItemDetailError.cantSaveTags -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_item_detail_cant_save_tags
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = onDismissErrorDialog
                ),
                onDismiss = onDismissErrorDialog
            )
        }
        ItemDetailError.cantStoreChanges -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_item_detail_cant_save_changes
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = onDismissErrorDialog
                ),
                onDismiss = onDismissErrorDialog
            )
        }
        ItemDetailError.cantTrashItem -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_item_detail_cant_trash_item
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = onDismissErrorDialog
                ),
                onDismiss = onDismissErrorDialog
            )
        }
        is ItemDetailError.droppedFields -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.errors_item_detail_dropped_fields_title),
                description = droppedFieldsMessage(names = itemDetailError.fields),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = acceptPrompt
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.cancel),
                    onClick = cancelPrompt
                ),
                onDismiss = onDismissErrorDialog
            )
        }
        is ItemDetailError.typeNotSupported -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.error),
                description = stringResource(
                    id = Strings.errors_item_detail_unsupported_type,
                    itemDetailError.type
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = onDismissErrorDialog
                ),
                onDismiss = onDismissErrorDialog
            )
        }
        is ItemDetailError.itemWasChangedRemotely -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.warning),
                description = stringResource(
                    id = Strings.item_detail_data_reloaded,
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = acceptItemWasChangedRemotely
                ),
                onDismiss = onDismissErrorDialog
            )
        }
        is ItemDetailError.askUserToDeleteOrRestoreItem -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.item_detail_deleted_title),
                description = stringResource(
                    id = Strings.item_detail_deleted_message,
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
    }
}

@Composable
private fun droppedFieldsMessage(names: List<String>): String {
    val formattedNames = names.joinToString(separator = "") { "- $it\n" }
    return stringResource(id = Strings.errors_item_detail_dropped_fields_message, formattedNames)
}