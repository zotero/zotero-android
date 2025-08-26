package org.zotero.android.pdf.pdffilter

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun PdfFilterTagsListAndSelect(
    viewState: PdfFilterViewState,
    viewModel: PdfFilterViewModel,
) {
    if (!viewState.availableTags.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .safeClickable(
                    onClick = viewModel::onTagsClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val formattedTags = viewState.formattedTags()
            Text(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                text = formattedTags.ifEmpty {
                    stringResource(id = Strings.pdf_annotations_sidebar_filter_tags_placeholder)
                },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}