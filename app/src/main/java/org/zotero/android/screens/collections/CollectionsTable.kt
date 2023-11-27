package org.zotero.android.screens.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.badge.RoundBadgeIcon
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.misc.NewDivider
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
    levelPadding: Dp = 8.dp
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
    var rowModifier: Modifier = Modifier.height(44.dp)
    if (layoutType.isTablet() && viewState.selectedCollectionId == item.collection.identifier) {
        rowModifier = rowModifier.background(color = CustomTheme.colors.popupSelectedRow)
    }
    val arrowIconAreaSize = 32.dp
    val mainIconSize = 28.dp
    val paddingBetweenIconAndText = 16.dp
    val levelPaddingWithArrowIconAreaSize = levelPadding + arrowIconAreaSize
    val dividerOffset = levelPaddingWithArrowIconAreaSize + mainIconSize + paddingBetweenIconAndText
    Box {
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
                Spacer(modifier = Modifier.width(levelPaddingWithArrowIconAreaSize))
            } else {
                Spacer(modifier = Modifier.width(levelPadding))
                IconWithPadding(
                    drawableRes = if (viewState.isCollapsed(item)) {
                        Drawables.chevron_right_24px
                    } else {
                        Drawables.expand_more_24px
                    },
                    onClick = { viewModel.onItemChevronTapped(item.collection) },
                    areaSize = arrowIconAreaSize,
                    shouldShowRipple = false
                )
            }
            Icon(
                modifier = Modifier.size(mainIconSize),
                painter = painterResource(id = item.collection.iconName),
                contentDescription = null,
                tint = CustomTheme.colors.zoteroDefaultBlue
            )
            Spacer(modifier = Modifier.width(paddingBetweenIconAndText))

            Text(
                modifier = Modifier.weight(1f),
                text = item.collection.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = CustomTheme.typography.newBody,
                color = CustomTheme.colors.allItemsRowTitleColor,
            )
            Spacer(modifier = Modifier.width(16.dp))
            if ((!item.collection.isCollection || viewModel.defaults.showCollectionItemCounts()) && item.collection.itemCount != 0) {
                RoundBadgeIcon(count = item.collection.itemCount)
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
        NewDivider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = dividerOffset)
        )
    }
}

