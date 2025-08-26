package org.zotero.android.pdf.reader.pdfsearch.row

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.reader.pdfsearch.data.PdfReaderSearchItem
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.debounceCombinedClickable

@Composable
internal fun PdfReaderSearchRow(
    searchItem: PdfReaderSearchItem,
    onItemTapped: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .debounceCombinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onItemTapped,
            )
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Strings.page) + " ${searchItem.pageNumber + 1}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier,
            text = searchItem.annotatedString,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}