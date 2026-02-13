package org.zotero.android.screens.htmlepub.reader.sidebar.rows

import androidx.compose.runtime.Composable
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewState
import org.zotero.android.screens.htmlepub.reader.sidebar.sections.HtmlEpubReaderAnnotationsSidebarTagsAndCommentsSection

@Composable
internal fun HtmlEpubReaderAnnotationsSidebarNoteRow(
    annotation: PDFAnnotation,
    viewModel: HtmlEpubReaderViewModel,
    viewState: HtmlEpubReaderViewState,
) {
    HtmlEpubReaderAnnotationsSidebarTagsAndCommentsSection(
        annotation = annotation,
        viewState = viewState,
        viewModel = viewModel,
        shouldAddTopPadding = true,
    )
}