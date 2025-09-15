package org.zotero.android.screens.collections.rows

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.collections.CollectionsViewModel
import org.zotero.android.screens.collections.CollectionsViewState
import org.zotero.android.sync.CollectionIdentifier

internal fun LazyListScope.fixedCollectionRow(
    customType: CollectionIdentifier.CustomType,
    viewState: CollectionsViewState,
    layoutType: CustomLayoutSize.LayoutType,
    viewModel: CollectionsViewModel
) {
    item {
        val fixedCollection = viewState.fixedCollections[customType]
        if (fixedCollection != null) {
            CollectionRowItem(
                layoutType = layoutType,
                levelPadding = 4.dp,
                selectedCollectionId = viewState.selectedCollectionId,
                collection = fixedCollection,
                hasChildren = false,
                showCollectionItemCounts = viewState.showCollectionItemCounts,
                isCollapsed = true,
                onItemTapped = { viewModel.onItemTapped(fixedCollection) },
                onItemLongTapped = { viewModel.onItemLongTapped(fixedCollection) },
                onItemChevronTapped = { viewModel.onItemChevronTapped(fixedCollection) }
            )
        }
    }
}