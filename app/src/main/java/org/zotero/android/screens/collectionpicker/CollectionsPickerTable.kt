package org.zotero.android.screens.collectionpicker

import androidx.annotation.DrawableRes
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
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
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
            layoutType = layoutType,
            collectionItems = viewState.collectionItemsToDisplay,
            isChecked = { viewState.selected.contains(it.collection.identifier.keyGet) },
            onClick = { viewModel.selectOrDeselect(it.collection) },
        )
    }
}

private fun LazyListScope.recursiveCollectionItem(
    layoutType: CustomLayoutSize.LayoutType,
    levelPadding: Dp = 12.dp,
    collectionItems: ImmutableList<CollectionItemWithChildren>,
    isChecked: (item: CollectionItemWithChildren) -> Boolean,
    onClick: (item: CollectionItemWithChildren) -> Unit
) {
    for (item in collectionItems) {
        item {
            CollectionItem(
                iconName = item.collection.iconName,
                collectionName = item.collection.name,
                layoutType = layoutType,
                levelPadding = levelPadding,
                isChecked = isChecked(item),
                onClick = { onClick(item) },
            )
        }

        recursiveCollectionItem(
            layoutType = layoutType,
            levelPadding = levelPadding + levelPaddingConst,
            collectionItems = item.children,
            isChecked = isChecked,
            onClick = onClick,
        )
    }
}

@Composable
private fun CollectionItem(
    @DrawableRes iconName: Int,
    collectionName: String,
    layoutType: CustomLayoutSize.LayoutType,
    levelPadding: Dp,
    isChecked: Boolean,
    onClick: () -> Unit,
) {
    var rowModifier: Modifier = Modifier
    if (isChecked) {
        rowModifier = rowModifier.background(color = CustomTheme.colors.popupSelectedRow)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
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
            painter = painterResource(id = iconName),
            contentDescription = null,
            tint = CustomTheme.colors.zoteroDefaultBlue
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = collectionName,
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