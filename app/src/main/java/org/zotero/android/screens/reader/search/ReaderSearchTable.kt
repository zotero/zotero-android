package org.zotero.android.screens.reader.search

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import org.zotero.android.screens.reader.search.row.ReaderSearchRow

internal fun LazyListScope.readerSearchTable(
    viewState: ReaderSearchViewState,
    viewModel: ReaderSearchViewModel,
) {
    items(viewState.searchResults) { item ->
        ReaderSearchRow(searchItem = item, onItemTapped = { viewModel.onItemTapped(item) })
    }
}

