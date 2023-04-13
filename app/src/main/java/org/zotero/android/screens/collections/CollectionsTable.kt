package org.zotero.android.screens.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.badge.RoundBadgeIcon
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme

private val levelPaddingConst = 8.dp

@Composable
internal fun CollectionsTable(
    viewState: CollectionsViewState,
    viewModel: CollectionsViewModel,
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
    viewState: CollectionsViewState,
    viewModel: CollectionsViewModel,
    layoutType: CustomLayoutSize.LayoutType,
    collectionItems: List<CollectionItemWithChildren>,
    levelPadding: Dp = 36.dp
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

        if (!viewState.isCollapsed(item)) {
            recursiveCollectionItem(
                viewState = viewState,
                viewModel = viewModel,
                layoutType = layoutType,
                collectionItems = item.children,
                levelPadding = levelPadding + levelPaddingConst
            )
        }
    }
}

@Composable
private fun CollectionItem(
    viewState: CollectionsViewState,
    viewModel: CollectionsViewModel,
    item: CollectionItemWithChildren,
    layoutType: CustomLayoutSize.LayoutType,
    levelPadding: Dp
) {
    var rowModifier: Modifier = Modifier
    if (layoutType.isTablet() && viewState.selectedCollectionId == item.collection.identifier) {
        rowModifier = rowModifier.background(color = CustomTheme.colors.popupSelectedRow)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = { viewModel.onItemTapped(item.collection) },
                onLongClick = { viewModel.onItemLongTapped(item.collection) }
            )
    ) {
        val hasChildren = item.children.isNotEmpty()
        if (!hasChildren) {
            Spacer(modifier = Modifier.width(levelPadding))
        } else {
            val paddingBetweenArrowAndItemIcon = 8.dp
            val arrowIconSize = 22.dp
            val leftPadding = levelPadding - arrowIconSize - paddingBetweenArrowAndItemIcon
            Spacer(modifier = Modifier.width(leftPadding))
            Row(
                modifier = Modifier.safeClickable(
                    onClick = { viewModel.onItemChevronTapped(item.collection) })
            ) {
                Icon(
                    modifier = Modifier.size(arrowIconSize),
                    painter = painterResource(
                        id = if (viewState.isCollapsed(item)) {
                            Drawables.baseline_keyboard_arrow_right_24
                        } else {
                            Drawables.baseline_keyboard_arrow_down_24
                        }
                    ),
                    contentDescription = null,
                    tint = CustomTheme.colors.zoteroBlueWithDarkMode
                )
                Spacer(modifier = Modifier.width(paddingBetweenArrowAndItemIcon))
            }
        }
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
                if ((!item.collection.isCollection || viewModel.defaults.showCollectionItemCounts()) && item.collection.itemCount != 0) {
                    Row {
                        RoundBadgeIcon(count = item.collection.itemCount)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            CustomDivider()
        }
    }

}

