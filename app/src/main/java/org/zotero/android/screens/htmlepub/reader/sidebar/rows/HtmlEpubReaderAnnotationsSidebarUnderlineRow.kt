package org.zotero.android.screens.htmlepub.reader.sidebar.rows

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewState
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import org.zotero.android.screens.htmlepub.reader.sidebar.sections.HtmlEpubReaderAnnotationsSidebarTagsAndCommentsSection
import org.zotero.android.screens.htmlepub.reader.sidebar.sections.HtmlEpubReaderAnnotationsSidebarUnderlineTextSection

@Composable
internal fun HtmlEpubReaderAnnotationsSidebarUnderlineRow(
    annotation: HtmlEpubAnnotation,
    viewState: HtmlEpubReaderViewState,
    viewModel: HtmlEpubReaderViewModel,
    annotationColor: Color,
) {
    HtmlEpubReaderAnnotationsSidebarUnderlineTextSection(annotationColor = annotationColor, annotation = annotation)

    HtmlEpubReaderAnnotationsSidebarTagsAndCommentsSection(
        annotation = annotation,
        viewState = viewState,
        viewModel = viewModel,
        shouldAddTopPadding = false,
    )
}