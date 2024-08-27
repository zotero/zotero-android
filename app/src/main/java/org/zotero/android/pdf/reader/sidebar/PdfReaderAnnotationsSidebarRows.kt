package org.zotero.android.pdf.reader.sidebar

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.zotero.android.pdf.data.Annotation
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState

@Composable
internal fun PdfReaderAnnotationsSidebarImageRow(
    viewState: PdfReaderViewState,
    vMInterface: PdfReaderVMInterface,
    annotation: Annotation,
    loadPreview: () -> Bitmap?,
) {
    PdfReaderAnnotationsSidebarImageSection(
        loadPreview = loadPreview,
        vMInterface = vMInterface,
    )
    SidebarDivider()
    PdfReaderAnnotationsSidebarTagsAndCommentsSection(
        vMInterface = vMInterface,
        viewState = viewState,
        annotation = annotation,
        shouldAddTopPadding = true,
    )
}

@Composable
internal fun PdfReaderAnnotationsSidebarInkRow(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    annotation: Annotation,
    loadPreview: () -> Bitmap?,
) {
    PdfReaderAnnotationsSidebarImageSection(loadPreview = loadPreview, vMInterface = vMInterface)
    PdfReaderAnnotationsSidebarTagsSection(vMInterface = vMInterface, viewState = viewState, annotation = annotation)
}

@Composable
internal fun PdfReaderAnnotationsSidebarNoteRow(
    annotation: Annotation,
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

@Composable
internal fun PdfReaderAnnotationsSidebarHighlightRow(
    annotation: Annotation,
    viewState: PdfReaderViewState,
    vMInterface: PdfReaderVMInterface,
    annotationColor: Color,
) {
    PdfReaderAnnotationsSidebarHighlightedTextSection(annotationColor = annotationColor, annotation = annotation)

    PdfReaderAnnotationsSidebarTagsAndCommentsSection(
        annotation = annotation,
        viewState = viewState,
        vMInterface = vMInterface,
        shouldAddTopPadding = false,
    )
}
