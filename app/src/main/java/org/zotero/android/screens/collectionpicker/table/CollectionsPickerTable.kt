package org.zotero.android.screens.collectionpicker.table

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.collectionpicker.CollectionPickerViewModel
import org.zotero.android.screens.collectionpicker.CollectionPickerViewState

@Composable
internal fun CollectionsPickerTable(
    viewState: CollectionPickerViewState,
    viewModel: CollectionPickerViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {
    LazyColumn(
        state = rememberLazyListState(),
    ) {
        collectionsPickerRecursiveCollectionItem(
            layoutType = layoutType,
            collectionItems = viewState.collectionItemsToDisplay,
            isChecked = { viewState.selected.contains(it.collection.identifier.keyGet) },
            onClick = { viewModel.selectOrDeselect(it.collection) },
        )
    }
}