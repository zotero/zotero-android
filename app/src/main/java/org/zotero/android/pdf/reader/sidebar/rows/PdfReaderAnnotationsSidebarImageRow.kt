package org.zotero.android.pdf.reader.sidebar.rows

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.sidebar.sections.PdfReaderAnnotationsSidebarImageSection
import org.zotero.android.pdf.reader.sidebar.sections.PdfReaderAnnotationsSidebarTagsAndCommentsSection

@Composable
internal fun PdfReaderAnnotationsSidebarImageRow(
    viewState: PdfReaderViewState,
    vMInterface: PdfReaderVMInterface,
    annotation: PDFAnnotation,
    loadPreview: () -> Bitmap?,
) {
    PdfReaderAnnotationsSidebarImageSection(
        loadPreview = loadPreview,
        vMInterface = vMInterface,
    )
    PdfReaderAnnotationsSidebarTagsAndCommentsSection(
        vMInterface = vMInterface,
        viewState = viewState,
        annotation = annotation,
        shouldAddTopPadding = true,
    )
}
