package org.zotero.android.screens.allitems

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.misc.NewDivider
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
        )
    } else {
        BottomPanel(
            modifier = commonModifier,
            viewModel = viewModel,
            viewState = viewState,
        )
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
            if (viewState.isCollectionTrash) {
                val isRestoreAndDeleteEnabled = viewState.selectedItems.isNotEmpty()
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
            } else {
                val isDeleteEnabled = viewState.selectedItems.isNotEmpty()
                IconWithPadding(
                    drawableRes = Drawables.delete_24px,
                    isEnabled = isDeleteEnabled,
                    tintColor = if (isDeleteEnabled) CustomTheme.colors.zoteroDefaultBlue else CustomTheme.colors.disabledContent,
                    onClick = {
                        viewModel.onTrash()
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomPanel(
    modifier: Modifier,
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
) {
    Box(
        modifier = modifier
    ) {
        NewDivider(modifier = Modifier.align(Alignment.TopStart))

        IconWithPadding(
            modifier = Modifier
                .padding(end = 20.dp)
                .align(Alignment.CenterEnd),
            drawableRes = Drawables.swap_vert_24px,
            onClick = {
                viewModel.showSortPicker()
            }
        )
        val filterDrawable =
            if (viewState.filters.isEmpty()) {
                Drawables.filter_list_off_24px
            } else {
                Drawables.filter_list_24px
            }
        IconWithPadding(
            modifier = Modifier
                .padding(start = 20.dp)
                .align(Alignment.CenterStart),
            drawableRes = filterDrawable,
            onClick = {
                viewModel.showFilters()
            }
        )
    }
}
