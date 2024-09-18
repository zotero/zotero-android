package org.zotero.android.pdf.reader.sidebar.rows

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.sidebar.PdfReaderAnnotationsSidebarTagsAndCommentsSection
import org.zotero.android.pdf.reader.sidebar.PdfReaderAnnotationsSidebarUnderlineTextSection

@Composable
internal fun PdfReaderAnnotationsSidebarUnderlineRow(
    annotation: PDFAnnotation,
    viewState: PdfReaderViewState,
    vMInterface: PdfReaderVMInterface,
    annotationColor: Color,
) {
    PdfReaderAnnotationsSidebarUnderlineTextSection(annotationColor = annotationColor, annotation = annotation)

    PdfReaderAnnotationsSidebarTagsAndCommentsSection(
        annotation = annotation,
        viewState = viewState,
        vMInterface = vMInterface,
        shouldAddTopPadding = false,
    )
}