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
                            id = Strings.cantCreateAttachmentsWithNames,
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
                            id = Strings.cantCreateAttachmentsWithNames,
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
                    id = Strings.cantLoadData
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
                    id = Strings.unknown
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
                    id = Strings.cantSaveNotes
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
                    id = Strings.cantSaveTags
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
                    id = Strings.cantStoreChanges
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
                    id = Strings.cantTrashItem
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
                title = stringResource(id = Strings.droppedFieldsTitle),
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
                    id = Strings.unsupportedType,
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
                    id = Strings.dataReloaded,
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
                title = stringResource(id = Strings.deletedTitle),
                description = stringResource(
                    id = Strings.deletedMessage,
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
    val formattedNames = names.map { "- $it\n" }.joinToString(separator = "")
    return stringResource(id = Strings.droppedFieldsMessage, formattedNames)
}