package org.zotero.android.pdf.reader.pdfsearch

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.reader.pdfsearch.data.PdfReaderSearchItem
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme

internal fun LazyListScope.pdfReaderSearchTable(
    viewState: PdfReaderSearchViewState,
    viewModel: PdfReaderSearchViewModel,
) {
    items(viewState.searchResults) { item ->
        PdfReaderSearchRow(searchItem = item, onItemTapped = { viewModel.onItemTapped(item) })
    }
}

@Composable
private fun PdfReaderSearchRow(
    searchItem: PdfReaderSearchItem,
    onItemTapped: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onItemTapped,
            )
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier.align(Alignment.End),
            text = stringResource(Strings.page) + " ${searchItem.pageNumber + 1}",
            style = CustomTheme.typography.newCaptionOne,
            color = CustomTheme.colors.zoteroBlueWithDarkMode
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier,
            text = searchItem.annotatedString,
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.defaultTextColor,
        )
        Spacer(modifier = Modifier.height(8.dp))
        NewDivider(
            modifier = Modifier
        )
    }
}