package org.zotero.android.pdf.reader.pdfsearch.row

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.reader.pdfsearch.PdfReaderSearchViewState
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.foundation.quantityStringResource

@Composable
internal fun PdfReaderSearchFoundMatchesRow(viewState: PdfReaderSearchViewState) {
    Row(
        modifier = Modifier.height(48.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = quantityStringResource(
                id = Plurals.pdf_search_matches, viewState.searchResults.size
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}