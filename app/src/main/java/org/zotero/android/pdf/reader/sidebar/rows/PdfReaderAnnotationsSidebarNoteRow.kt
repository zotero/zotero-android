package org.zotero.android.pdf.reader.sidebar.rows

import androidx.compose.runtime.Composable
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.sidebar.PdfReaderAnnotationsSidebarTagsAndCommentsSection

@Composable
internal fun PdfReaderAnnotationsSidebarNoteRow(
    annotation: PDFAnnotation,
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
) {
    PdfReaderAnnotationsSidebarTagsAndCommentsSection(
        annotation = annotation,
        viewState = viewState,
        vMInterface = vMInterface,
        shouldAddTopPadding = true,
    )
}