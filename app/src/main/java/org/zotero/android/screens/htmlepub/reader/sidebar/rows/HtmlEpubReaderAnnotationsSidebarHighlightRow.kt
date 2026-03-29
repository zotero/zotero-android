package org.zotero.android.screens.htmlepub.reader.sidebar.rows

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewState
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import org.zotero.android.screens.htmlepub.reader.sidebar.sections.HtmlEpubReaderAnnotationsSidebarHighlightedTextSection
import org.zotero.android.screens.htmlepub.reader.sidebar.sections.HtmlEpubReaderAnnotationsSidebarTagsAndCommentsSection

@Composable
internal fun HtmlEpubReaderAnnotationsSidebarHighlightRow(
    annotation: HtmlEpubAnnotation,
    viewState: HtmlEpubReaderViewState,
    viewModel: HtmlEpubReaderViewModel,
    annotationColor: Color,
) {
    HtmlEpubReaderAnnotationsSidebarHighlightedTextSection(annotationColor = annotationColor, annotation = annotation)

    HtmlEpubReaderAnnotationsSidebarTagsAndCommentsSection(
        annotation = annotation,
        viewState = viewState,
        viewModel = viewModel,
        shouldAddTopPadding = false,
    )
}