package org.zotero.android.screens.allitems

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun AllItemsBottomPanelNew(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
    scrollBehavior: BottomAppBarScrollBehavior,
) {
    if (viewState.isEditing) {
        if (viewState.selectedKeys?.size == 1) {
            AllItemsSingleItemEditingBottomPanel(
                viewModel = viewModel,
                viewState = viewState,
                scrollBehavior = scrollBehavior
            )
        }
    } else {
        AllItemsRegularBottomPanel(
            scrollBehavior = scrollBehavior,
            viewModel = viewModel,
            viewState = viewState,
        )
    }
}

@Composable
internal fun BoxScope.AllItemsBottomPanel(
    layoutType: CustomLayoutSize.LayoutType,
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel
) {
    val commonModifier = Modifier
        .fillMaxWidth()
//        .height(layoutType.calculateAllItemsBottomPanelHeight())
        .align(Alignment.BottomStart)
        .background(Color.Red)
    if (viewState.selectedKeys != null) {
        EditingBottomPanel(
            modifier = commonModifier,
            viewState = viewState,
            viewModel = viewModel,
        )
    } else {
//        BottomPanelV2(
//            modifier = commonModifier,
//            viewModel = viewModel,
//            viewState = viewState,
//        )
    }
}

@Composable
private fun EditingBottomPanel(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        NewDivider()
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isRestoreAndDeleteEnabled = viewState.isAnythingSelected()
            if (viewState.isCollectionTrash) {
                IconWithPadding(
                    drawableRes = Drawables.restore_trash,
                    isEnabled = isRestoreAndDeleteEnabled,
                    tintColor = if (isRestoreAndDeleteEnabled) {
                        CustomTheme.colors.zoteroDefaultBlue
                    } else {
                        CustomTheme.colors.disabledContent
                    },
                    onClick = viewModel::onRestore
                )
                IconWithPadding(
                    drawableRes = Drawables.delete_24px,
                    isEnabled = isRestoreAndDeleteEnabled,
                    tintColor = if (isRestoreAndDeleteEnabled) {
                        CustomTheme.colors.zoteroDefaultBlue
                    } else {
                        CustomTheme.colors.disabledContent
                    },
                    onClick = {
                        viewModel.onDelete()
                    }
                )

                DownloadAndRemoveAttachmentBlock(
                    viewModel = viewModel,
                    isRestoreAndDeleteEnabled = isRestoreAndDeleteEnabled
                )
            } else {
                IconWithPadding(
                    drawableRes = Drawables.create_new_folder_24px,
                    isEnabled = isRestoreAndDeleteEnabled,
                    tintColor = if (isRestoreAndDeleteEnabled) CustomTheme.colors.zoteroDefaultBlue else CustomTheme.colors.disabledContent,
                    onClick = {
                        viewModel.onAddToCollection()
                    }
                )
                if (viewState.isCollectionACollection) {
                    IconWithPadding(
                        drawableRes = Drawables.remove_from_collection,
                        isEnabled = isRestoreAndDeleteEnabled,
                        tintColor = if (isRestoreAndDeleteEnabled) CustomTheme.colors.zoteroDefaultBlue else CustomTheme.colors.disabledContent,
                        onClick = {
                            viewModel.showRemoveFromCollectionQuestion()
                        }
                    )
                }

                IconWithPadding(
                    drawableRes = Drawables.delete_24px,
                    isEnabled = isRestoreAndDeleteEnabled,
                    tintColor = if (isRestoreAndDeleteEnabled) CustomTheme.colors.zoteroDefaultBlue else CustomTheme.colors.disabledContent,
                    onClick = {
                        viewModel.onTrash()
                    }
                )

                DownloadAndRemoveAttachmentBlock(
                    viewModel = viewModel,
                    isRestoreAndDeleteEnabled = isRestoreAndDeleteEnabled
                )
            }
        }
    }
}

@Composable
private fun DownloadAndRemoveAttachmentBlock(
    viewModel: AllItemsViewModel,
    isRestoreAndDeleteEnabled: Boolean
) {
    IconWithPadding(
        isEnabled = isRestoreAndDeleteEnabled,
        drawableRes = Drawables.download_24px,
        tintColor = if (isRestoreAndDeleteEnabled) {
            CustomTheme.colors.zoteroDefaultBlue
        } else {
            CustomTheme.colors.disabledContent
        },
        onClick = {
            viewModel.onDownloadSelectedAttachments()
        }
    )
    IconWithPadding(
        isEnabled = isRestoreAndDeleteEnabled,
        drawableRes = Drawables.file_download_off_24px,
        tintColor = if (isRestoreAndDeleteEnabled) {
            CustomTheme.colors.zoteroDefaultBlue
        } else {
            CustomTheme.colors.disabledContent
        },
        onClick = {
            viewModel.onRemoveSelectedAttachments()
        }
    )
}


//
//private fun allItemsTopBarActions(
//    viewState: AllItemsViewState,
//    viewModel: AllItemsViewModel
//): List<@Composable (RowScope.() -> Unit)> {
//    val buttonsList :MutableList<@Composable (RowScope.() -> Unit)> = mutableListOf()
//    if (viewState.isCollectionTrash) {
//        buttonsList.add {
//            NewHeadingTextButton(
//                onClick = viewModel::onEmptyTrash,
//                text = stringResource(Strings.collections_empty_trash),
//            )
//        }
//    } else {
//        buttonsList.add {
//            IconWithPadding(
//                drawableRes = Drawables.add_24px,
//                onClick = viewModel::onAdd,
//                shouldShowRipple = false
//            )
//        }
//    }
//    if (!viewState.isEditing) {
//        buttonsList.add {
//            NewHeadingTextButton(
//                onClick = viewModel::onSelect,
//                text = stringResource(Strings.select),
//            )
//        }
//    } else {
//        val allSelected = viewState.areAllSelected
//        if (allSelected) {
//            buttonsList.add {
//                NewHeadingTextButton(
//                    onClick = viewModel::toggleSelectionState,
//                    text = stringResource(Strings.items_deselect_all),
//                )
//            }
//        } else {
//            buttonsList.add {
//                NewHeadingTextButton(
//                    onClick = viewModel::toggleSelectionState,
//                    text = stringResource(Strings.items_select_all),
//                )
//            }
//        }
//        buttonsList.add {
//            NewHeadingTextButton(
//                onClick = viewModel::onDone,
//                text = stringResource(Strings.done),
//            )
//        }
//    }
//    return buttonsList
//}

