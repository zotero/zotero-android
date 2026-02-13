package org.zotero.android.screens.htmlepub.reader.sidebar.sections

import androidx.compose.runtime.Composable
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewState

@Composable
internal fun HtmlEpubReaderAnnotationsSidebarTagsAndCommentsSection(
    viewState: HtmlEpubReaderViewState,
    viewModel: HtmlEpubReaderViewModel,
    annotation: PDFAnnotation,
    shouldAddTopPadding: Boolean,
) {
    HtmlEpubReaderAnnotationsSidebarCommentSection(
        annotation = annotation,
        shouldAddTopPadding = shouldAddTopPadding,
        viewModel = viewModel,
        viewState = viewState,
    )
    HtmlEpubReaderAnnotationsSidebarTagsSection(
        annotation = annotation,
        viewModel = viewModel,
        viewState = viewState,
    )
}
