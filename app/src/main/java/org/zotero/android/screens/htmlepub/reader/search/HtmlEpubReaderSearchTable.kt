package org.zotero.android.screens.htmlepub.reader.search

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import org.zotero.android.screens.htmlepub.reader.search.row.HtmlEpubReaderSearchRow

internal fun LazyListScope.htmlEpubReaderSearchTable(
    viewState: HtmlEpubReaderSearchViewState,
    viewModel: HtmlEpubReaderSearchViewModel,
) {
    items(viewState.searchResults) { item ->
        HtmlEpubReaderSearchRow(searchItem = item, onItemTapped = { viewModel.onItemTapped(item) })
    }
}

