package org.zotero.android.screens.collections

import androidx.compose.foundation.background
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
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.badge.RoundBadgeIcon
import org.zotero.android.uicomponents.foundation.debounceCombinedClickable
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
            layoutType = layoutType,
            collectionItems = viewState.collectionItemsToDisplay,
            selectedCollectionId = viewState.selectedCollectionId,
            isCollapsed = { viewModel.isCollapsed(it) },
            onItemTapped = { viewModel.onItemTapped(it.collection) },
            onItemLongTapped = { viewModel.onItemLongTapped(it.collection) },
            onItemChevronTapped = { viewModel.onItemChevronTapped(it.collection) },
            showCollectionItemCounts = viewModel.showCollectionItemCounts()
        )
    }
}


private fun LazyListScope.recursiveCollectionItem(
    layoutType: CustomLayoutSize.LayoutType,
    levelPadding: Dp = 8.dp,
    collectionItems: ImmutableList<CollectionItemWithChildren>,
    selectedCollectionId: CollectionIdentifier,
    showCollectionItemCounts: Boolean,
    isCollapsed: (item: CollectionItemWithChildren) -> Boolean,
    onItemTapped: (item: CollectionItemWithChildren) -> Unit,
    onItemLongTapped: (item: CollectionItemWithChildren) -> Unit,
    onItemChevronTapped: (item: CollectionItemWithChildren) -> Unit,
) {
    for (item in collectionItems) {
        item {
            CollectionItem(
                layoutType = layoutType,
                levelPadding = levelPadding,
                selectedCollectionId = selectedCollectionId,
                collection = item.collection,
                hasChildren = item.children.isNotEmpty(),
                showCollectionItemCounts = showCollectionItemCounts,
                isCollapsed = isCollapsed(item),
                onItemTapped = { onItemTapped(item) },
                onItemLongTapped = { onItemLongTapped(item) },
                onItemChevronTapped = { onItemChevronTapped(item) }
            )
        }

        if (!isCollapsed(item)) {
            recursiveCollectionItem(
                layoutType = layoutType,
                levelPadding = levelPadding + levelPaddingConst,
                collectionItems = item.children,
                selectedCollectionId = selectedCollectionId,
                showCollectionItemCounts = showCollectionItemCounts,
                isCollapsed = isCollapsed,
                onItemTapped = onItemTapped,
                onItemLongTapped = onItemLongTapped,
                onItemChevronTapped = onItemChevronTapped
            )
        }
    }
}

@Composable
private fun CollectionItem(
    layoutType: CustomLayoutSize.LayoutType,
    levelPadding: Dp,
    collection: Collection,
    selectedCollectionId: CollectionIdentifier,
    showCollectionItemCounts: Boolean,
    hasChildren: Boolean,
    isCollapsed: Boolean,
    onItemTapped: () -> Unit,
    onItemLongTapped: () -> Unit,
    onItemChevronTapped: () -> Unit,
) {
    var rowModifier: Modifier = Modifier.height(44.dp)
    if (layoutType.isTablet() && selectedCollectionId == collection.identifier) {
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
                .debounceCombinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onItemTapped,
                    onLongClick = onItemLongTapped
                )
        ) {
            if (!hasChildren) {
                Spacer(modifier = Modifier.width(levelPaddingWithArrowIconAreaSize))
            } else {
                Spacer(modifier = Modifier.width(levelPadding))
                IconWithPadding(
                    drawableRes = if (isCollapsed) {
                        Drawables.chevron_right_24px
                    } else {
                        Drawables.expand_more_24px
                    },
                    onClick = { onItemChevronTapped() },
                    areaSize = arrowIconAreaSize,
                    shouldShowRipple = false
                )
            }
            Icon(
                modifier = Modifier.size(mainIconSize),
                painter = painterResource(id = collection.iconName),
                contentDescription = null,
                tint = CustomTheme.colors.zoteroDefaultBlue
            )
            Spacer(modifier = Modifier.width(paddingBetweenIconAndText))

            Text(
                modifier = Modifier.weight(1f),
                text = collection.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = CustomTheme.typography.newBody,
                color = CustomTheme.colors.allItemsRowTitleColor,
            )
            Spacer(modifier = Modifier.width(16.dp))
            if ((!collection.isCollection || showCollectionItemCounts) && collection.itemCount != 0) {
                RoundBadgeIcon(count = collection.itemCount)
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

