package org.zotero.android.pdf.annotationmore.blocks

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun PdfAnnotationMorePageButton(
    viewModel: PdfAnnotationMoreViewModel,
    viewState: PdfAnnotationMoreViewState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .safeClickable(
                onClick = viewModel::onPageClicked,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ), verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = stringResource(Strings.page_number),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = viewState.pageLabel,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

}
