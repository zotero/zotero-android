package org.zotero.android.pdf.pdffilter

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfFilterTagsListAndSelect(
    viewState: PdfFilterViewState,
    viewModel: PdfFilterViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {
    if (!viewState.availableTags.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 20.dp)
                .safeClickable(
                    onClick = viewModel::onTagsClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val formattedTags = viewState.formattedTags()
            Text(
                modifier = Modifier.weight(1f),
                text = if (formattedTags.isEmpty()) {
                    stringResource(id = Strings.pdf_annotations_sidebar_filter_tags_placeholder)
                } else {
                    formattedTags
                },
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.default,
                fontSize = layoutType.calculateTextSize(),
            )

            Icon(
                painter = painterResource(Drawables.ic_arrow_small_right),
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp),
                tint = CustomTheme.colors.zoteroDefaultBlue,
            )
        }

    }
}