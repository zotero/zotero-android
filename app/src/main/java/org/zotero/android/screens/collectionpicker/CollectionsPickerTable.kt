package org.zotero.android.screens.collectionpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
import org.zotero.android.uicomponents.checkbox.CircleCheckBox
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme

private val levelPaddingConst = 8.dp

@Composable
internal fun CollectionsPickerTable(
    viewState: CollectionPickerViewState,
    viewModel: CollectionPickerViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {
    LazyColumn(
        state = rememberLazyListState(),
    ) {
        recursiveCollectionItem(
            viewState = viewState,
            viewModel = viewModel,
            layoutType = layoutType,
            collectionItems = viewState.collectionItemsToDisplay
        )
    }
}

private fun LazyListScope.recursiveCollectionItem(
    viewState: CollectionPickerViewState,
    viewModel: CollectionPickerViewModel,
    layoutType: CustomLayoutSize.LayoutType,
    collectionItems: List<CollectionItemWithChildren>,
    levelPadding: Dp = 12.dp
) {
    for (item in collectionItems) {
        item {
            CollectionItem(
                item = item,
                layoutType = layoutType,
                viewState = viewState,
                viewModel = viewModel,
                levelPadding = levelPadding,
            )
        }

        recursiveCollectionItem(
            viewState = viewState,
            viewModel = viewModel,
            layoutType = layoutType,
            collectionItems = item.children,
            levelPadding = levelPadding + levelPaddingConst
        )
    }
}

@Composable
private fun CollectionItem(
    viewState: CollectionPickerViewState,
    viewModel: CollectionPickerViewModel,
    item: CollectionItemWithChildren,
    layoutType: CustomLayoutSize.LayoutType,
    levelPadding: Dp
) {
    val isChecked = viewState.selected.contains(item.collection.identifier.keyGet)
    var rowModifier: Modifier = Modifier
    if (isChecked) {
        rowModifier = rowModifier.background(color = CustomTheme.colors.popupSelectedRow)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = { viewModel.selectOrDeselect(item.collection) },
            )
    ) {
        Spacer(modifier = Modifier.width(levelPadding))
        CircleCheckBox(
            isChecked = isChecked,
            layoutType = layoutType
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            modifier = Modifier.size(layoutType.calculateItemsRowMainIconSize()),
            painter = painterResource(id = item.collection.iconName),
            contentDescription = null,
            tint = CustomTheme.colors.zoteroBlueWithDarkMode
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.collection.name,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            CustomDivider()
        }
    }
}