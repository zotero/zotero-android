package org.zotero.android.pdf.reader.sidebar

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import org.zotero.android.pdf.data.Annotation
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState

@Composable
internal fun SidebarImageRow(
    viewState: PdfReaderViewState,
    vMInterface: PdfReaderVMInterface,
    annotation: Annotation,
    loadPreview: () -> Bitmap?,
    focusRequester: FocusRequester,
) {
    SidebarImageSection(
        loadPreview = loadPreview,
        vMInterface = vMInterface,
    )
    SidebarDivider()
    SidebarTagsAndCommentsSection(
        vMInterface = vMInterface,
        viewState = viewState,
        annotation = annotation,
        focusRequester = focusRequester,
        shouldAddTopPadding = true,
    )
}

@Composable
internal fun SidebarInkRow(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    annotation: Annotation,
    loadPreview: () -> Bitmap?,
) {
    SidebarImageSection(loadPreview = loadPreview, vMInterface = vMInterface)
    SidebarTagsSection(vMInterface = vMInterface, viewState = viewState, annotation = annotation)
}

@Composable
internal fun SidebarNoteRow(
    annotation: Annotation,
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    focusRequester: FocusRequester,
) {
    SidebarTagsAndCommentsSection(
        annotation = annotation,
        viewState = viewState,
        vMInterface = vMInterface,
        focusRequester = focusRequester,
        shouldAddTopPadding = true,
    )
}

@Composable
internal fun SidebarHighlightRow(
    annotation: Annotation,
    viewState: PdfReaderViewState,
    vMInterface: PdfReaderVMInterface,
    annotationColor: Color,
    focusRequester: FocusRequester,
) {
    SidebarHighlightedTextSection(annotationColor = annotationColor, annotation = annotation)

    SidebarTagsAndCommentsSection(
        annotation = annotation,
        viewState = viewState,
        vMInterface = vMInterface,
        focusRequester = focusRequester,
        shouldAddTopPadding = false,
    )
}
