package org.zotero.android.screens.collections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.collections.rows.fixedCollectionRow
import org.zotero.android.screens.collections.rows.recursiveCollectionItem
import org.zotero.android.sync.CollectionIdentifier

@Composable
internal fun CollectionsTable(
    viewState: CollectionsViewState,
    viewModel: CollectionsViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {
    LazyColumn(
        state = rememberLazyListState(),
    ) {
        fixedCollectionRow(
            customType = CollectionIdentifier.CustomType.all,
            viewState = viewState,
            layoutType = layoutType,
            viewModel = viewModel
        )
        recursiveCollectionItem(
            layoutType = layoutType,
            collectionItems = viewState.collectionItemsToDisplay,
            selectedCollectionId = viewState.selectedCollectionId,
            isCollapsed = { viewState.isCollapsed(it) },
            onItemTapped = { viewModel.onItemTapped(it.collection) },
            onItemLongTapped = { viewModel.onItemLongTapped(it.collection) },
            onItemChevronTapped = { viewModel.onItemChevronTapped(it.collection) },
            showCollectionItemCounts = viewState.showCollectionItemCounts
        )
        fixedCollectionRow(
            customType = CollectionIdentifier.CustomType.unfiled,
            viewState = viewState,
            layoutType = layoutType,
            viewModel = viewModel
        )
        fixedCollectionRow(
            customType = CollectionIdentifier.CustomType.trash,
            viewState = viewState,
            layoutType = layoutType,
            viewModel = viewModel
        )
        if (!layoutType.isTablet()) {
            item {
                Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
            }
        }

    }
}


