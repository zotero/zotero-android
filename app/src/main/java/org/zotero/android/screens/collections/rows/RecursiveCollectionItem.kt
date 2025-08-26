package org.zotero.android.screens.collections.rows

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
import org.zotero.android.sync.CollectionIdentifier

private val levelPaddingConst = 16.dp

internal fun LazyListScope.recursiveCollectionItem(
    layoutType: CustomLayoutSize.LayoutType,
    levelPadding: Dp = 4.dp,
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
            CollectionRowItem(
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
