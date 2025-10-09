package org.zotero.android.pdf.reader.pdfsearch

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import org.zotero.android.pdf.reader.pdfsearch.row.PdfReaderSearchRow

internal fun LazyListScope.pdfReaderSearchTable(
    viewState: PdfReaderSearchViewState,
    viewModel: PdfReaderSearchViewModel,
) {
    items(viewState.searchResults) { item ->
        PdfReaderSearchRow(searchItem = item, onItemTapped = { viewModel.onItemTapped(item) })
    }
}

