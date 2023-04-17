package org.zotero.android.screens.allitems

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.requests.key
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun BoxScope.AllItemsBottomPanel(
    layoutType: CustomLayoutSize.LayoutType,
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel
) {
    val commonModifier = Modifier
        .fillMaxWidth()
        .height(layoutType.calculateAllItemsBottomPanelHeight())
        .align(Alignment.BottomStart)
    if (viewState.isEditing) {
        EditingBottomPanel(
            modifier = commonModifier,
            viewState = viewState,
            viewModel = viewModel,
            layoutType = layoutType
        )
    } else {
        BottomPanel(
            modifier = commonModifier,
            layoutType = layoutType,
            viewModel = viewModel
        )
    }
}

@Composable
private fun EditingBottomPanel(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel,
    layoutType: CustomLayoutSize.LayoutType,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        CustomDivider()
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (viewState.collection.identifier.isTrash) {
                val isRestoreAndDeleteEnabled = !viewState.selectedItems.isEmpty()
                Icon(
                    modifier = Modifier
                        .size(layoutType.calculateItemsBottomSheetIconSize())
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            onClick = {
                                viewModel.onRestore()
                            },
                            enabled = isRestoreAndDeleteEnabled
                        ),
                    painter = painterResource(id = Drawables.restore_trash),
                    contentDescription = null,
                    tint = if (isRestoreAndDeleteEnabled) {
                        CustomTheme.colors.zoteroBlueWithDarkMode
                    } else {
                        CustomTheme.colors.disabledContent
                    }
                )
                Icon(
                    modifier = Modifier
                        .size(layoutType.calculateItemsBottomSheetIconSize())
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            onClick = {
                                viewModel.onDelete()
                            },
                            enabled = isRestoreAndDeleteEnabled
                        ),
                    painter = painterResource(id = Drawables.empty_trash),
                    contentDescription = null,
                    tint = if (isRestoreAndDeleteEnabled) {
                        CustomTheme.colors.zoteroBlueWithDarkMode
                    } else {
                        CustomTheme.colors.disabledContent
                    }
                )
            } else {
                var isDuplicateEnabled = false
                if (viewState.selectedItems.size == 1) {
                    val key = viewState.selectedItems.firstOrNull()
                    if (key != null) {
                        val rItem = viewModel.results?.where()?.key(key)?.findFirst()
                        if (rItem != null && rItem.rawType != ItemTypes.attachment && rItem.rawType != ItemTypes.note) {
                            isDuplicateEnabled = true
                        }
                    }
                }

                Icon(
                    modifier = Modifier
                        .size(layoutType.calculateItemsBottomSheetIconSize())
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            onClick = {
                                viewModel.onDuplicate()
                            },
                            enabled = isDuplicateEnabled
                        ),
                    painter = painterResource(id = Drawables.baseline_content_copy_24),
                    contentDescription = null,
                    tint = if (isDuplicateEnabled) CustomTheme.colors.zoteroBlueWithDarkMode else CustomTheme.colors.disabledContent
                )
                val isDeleteEnabled = !viewState.selectedItems.isEmpty()
                Icon(
                    modifier = Modifier
                        .size(layoutType.calculateItemsBottomSheetIconSize())
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            onClick = {
                                viewModel.onTrash()
                            },
                            enabled = isDeleteEnabled
                        ),
                    painter = painterResource(id = Drawables.ic_delete_20dp),
                    contentDescription = null,
                    tint = if (isDeleteEnabled) CustomTheme.colors.zoteroBlueWithDarkMode else CustomTheme.colors.disabledContent
                )
            }

        }
    }
}

@Composable
private fun BottomPanel(
    modifier: Modifier,
    layoutType: CustomLayoutSize.LayoutType,
    viewModel: AllItemsViewModel
) {
    Box(
        modifier = modifier
    ) {
        CustomDivider(modifier = Modifier.align(Alignment.TopStart))
        Icon(
            modifier = Modifier
                .padding(end = 30.dp)
                .size(layoutType.calculateItemsBottomSheetIconSize())
                .align(Alignment.CenterEnd)
                .safeClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(),
                    onClick = {
                        viewModel.showSortPicker()
                    }
                ),
            painter = painterResource(id = Drawables.baseline_swap_vert_24),
            contentDescription = null,
            tint = CustomTheme.colors.zoteroBlueWithDarkMode
        )
        Icon(
            modifier = Modifier
                .padding(start = 30.dp)
                .size(layoutType.calculateItemsBottomSheetIconSize())
                .align(Alignment.CenterStart)
                .safeClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(),
                    onClick = {
                        viewModel.showFilters()
                    }
                ),
            painter = painterResource(id = Drawables.baseline_filter_list_24),
            contentDescription = null,
            tint = CustomTheme.colors.zoteroBlueWithDarkMode
        )
    }
}
