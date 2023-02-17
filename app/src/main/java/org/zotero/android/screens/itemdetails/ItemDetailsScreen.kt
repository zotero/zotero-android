@file:OptIn(ExperimentalPagerApi::class)

package org.zotero.android.screens.itemdetails

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.screens.itemdetails.data.ItemDetailError
import org.zotero.android.screens.itemdetails.data.ItemDetailField
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.bottomsheet.LongPressBottomSheet
import org.zotero.android.uicomponents.modal.CustomAlertDialog
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.HeadingTextButton
import java.io.File

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun ItemDetailsScreen(
    viewModel: ItemDetailsViewModel = hiltViewModel(),
    navigateToCreatorEditScreen: () -> Unit,
    navigateToCreatorEditDialog: () -> Unit,
    navigateToSinglePickerScreen: () -> Unit,
    navigateToSinglePickerDialog: () -> Unit,
    navigateToAddOrEditNote: () -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    onBack: () -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,

    ) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(ItemDetailsViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()

    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
            ItemDetailsViewEffect.ShowCreatorEditEffect -> {
                when (layoutType.showScreenOrDialog()) {
                    CustomLayoutSize.ScreenOrDialogToShow.SCREEN -> {
                        navigateToCreatorEditScreen()
                    }
                    CustomLayoutSize.ScreenOrDialogToShow.DIALOG -> {
                        navigateToCreatorEditDialog()
                    }
                }
            }
            ItemDetailsViewEffect.ShowItemTypePickerEffect -> {
                when (layoutType.showScreenOrDialog()) {
                    CustomLayoutSize.ScreenOrDialogToShow.SCREEN -> {
                        navigateToSinglePickerScreen()
                    }
                    CustomLayoutSize.ScreenOrDialogToShow.DIALOG -> {
                        navigateToSinglePickerDialog()
                    }
                }
            }
            ItemDetailsViewEffect.ScreenRefresh -> {
                //no-op
            }
            ItemDetailsViewEffect.OnBack -> {
                onBack()
            }
            ItemDetailsViewEffect.ShowAddOrEditNoteEffect -> {
                navigateToAddOrEditNote()
            }
            is ItemDetailsViewEffect.OpenFile -> {
                onOpenFile(consumedEffect.file, consumedEffect.mimeType)
            }
            is ItemDetailsViewEffect.OpenWebpage -> {
                onOpenWebpage(consumedEffect.uri)
            }
            is ItemDetailsViewEffect.ShowVideoPlayer -> {
                navigateToVideoPlayerScreen()
            }

            is ItemDetailsViewEffect.ShowImageViewer -> {
                navigateToImageViewerScreen()
            }

        }
    }
    CustomScaffold(
        topBar = {
            TopBar(
                onViewOrEditClicked = viewModel::onSaveOrEditClicked,
                onCancelOrBackClicked = viewModel::onCancelOrBackClicked,
                isEditing = viewState.isEditing
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = CustomTheme.colors.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(),
            ) {
                if (viewState.isEditing) {
                    ItemDetailsEditScreen(
                        viewState = viewState,
                        layoutType = layoutType,
                        viewModel = viewModel
                    )
                } else {
                    ItemDetailsViewScreen(
                        viewState = viewState,
                        layoutType = layoutType,
                        viewModel = viewModel
                    )
                }

            }
        }
        val itemDetailError = viewState.error
        if (itemDetailError != null) {
            ShowError(
                itemDetailError = itemDetailError,
                onDismissErrorDialog = viewModel::onDismissErrorDialog,
                onBack = onBack,
                acceptPrompt = viewModel::acceptPrompt,
                cancelPrompt = viewModel::cancelPrompt,
                acceptItemWasChangedRemotely = viewModel::acceptItemWasChangedRemotely,
            )
        }
        LongPressBottomSheet(
            layoutType = layoutType,
            longPressOptionsHolder = viewState.longPressOptionsHolder,
            onCollapse = viewModel::dismissBottomSheet,
            onOptionClick = viewModel::onLongPressOptionsItemSelected
        )
    }
}

@Composable
private fun ShowError(
    itemDetailError: ItemDetailError,
    onDismissErrorDialog: () -> Unit,
    onBack: () -> Unit,
    acceptPrompt: () -> Unit,
    cancelPrompt: () -> Unit,
    acceptItemWasChangedRemotely: () -> Unit,
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
        ItemDetailError.cantRemoveDuplicatedItem -> {
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
    }
}

@Composable
private fun droppedFieldsMessage(names: List<String>): String {
    val formattedNames = names.map { "- $it\n" }.joinToString(separator = "")
    return stringResource(id = Strings.droppedFieldsMessage, formattedNames)
}

@Composable
private fun TopBar(
    onViewOrEditClicked: () -> Unit,
    onCancelOrBackClicked: () -> Unit,
    isEditing: Boolean,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
) {

    TopAppBar(
        title = {
            HeadingTextButton(
                isEnabled = true,
                onClick = onCancelOrBackClicked,
                text = if (isEditing) stringResource(Strings.cancel) else stringResource(Strings.all_items)
            )
        },
        actions = {
            HeadingTextButton(
                isEnabled = true,
                onClick = onViewOrEditClicked,
                text = if (isEditing) stringResource(Strings.save) else stringResource(Strings.edit)
            )
            Spacer(modifier = Modifier.width(8.dp))
        },
        backgroundColor = CustomTheme.colors.surface,
        elevation = elevation,
    )

}

private sealed class CellType {
    data class field(val field: ItemDetailField) : CellType()
    data class creator(val creator: ItemDetailCreator) : CellType()
    data class value(val value: String, val title: String) : CellType()
}
