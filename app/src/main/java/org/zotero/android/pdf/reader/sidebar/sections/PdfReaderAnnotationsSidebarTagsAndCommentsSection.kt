package org.zotero.android.pdf.reader.sidebar.sections

import androidx.compose.runtime.Composable
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState

@Composable
internal fun PdfReaderAnnotationsSidebarTagsAndCommentsSection(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    annotation: PDFAnnotation,
    shouldAddTopPadding: Boolean,
) {
    PdfReaderAnnotationsSidebarCommentSection(
        annotation = annotation,
        shouldAddTopPadding = shouldAddTopPadding,
        vMInterface = vMInterface,
        viewState = viewState,
    )
    PdfReaderAnnotationsSidebarTagsSection(
        annotation = annotation,
        vMInterface = vMInterface,
        viewState = viewState,
    )
}
