package org.zotero.android.screens.htmlepub.reader.sidebar.sections

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewState
import org.zotero.android.screens.htmlepub.reader.sidebar.HtmlEpubReaderSidebarDivider
import org.zotero.android.screens.htmlepub.reader.sidebar.sectionHorizontalPadding
import org.zotero.android.screens.htmlepub.reader.sidebar.sectionVerticalPadding
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.debounceClickable

@Composable
internal fun HtmlEpubReaderAnnotationsSidebarTagsSection(
    annotation: PDFAnnotation,
    viewState: HtmlEpubReaderViewState,
    viewModel: HtmlEpubReaderViewModel,
) {
    val isSelected = viewState.isAnnotationSelected(annotation.key)
    val areTagsPresent = annotation.tags.isNotEmpty()
    val shouldDisplayTagsSection = (isSelected || areTagsPresent) && annotation.isZoteroAnnotation
    if (shouldDisplayTagsSection) {
        HtmlEpubReaderSidebarDivider()
    }
    if (shouldDisplayTagsSection) {
        Box(modifier = Modifier
            .debounceClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = { viewModel.onTagsClicked(annotation) }
            )
            .sectionVerticalPadding()
            .fillMaxWidth()
            .height(22.dp)
        ) {
            if (areTagsPresent) {
                Text(
                    modifier = Modifier
                        .sectionHorizontalPadding(),
                    text = annotation.tags.joinToString(separator = ", ") { it.name },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    modifier = Modifier
                        .sectionHorizontalPadding(),
                    text = stringResource(id = Strings.pdf_annotations_sidebar_add_tags),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}