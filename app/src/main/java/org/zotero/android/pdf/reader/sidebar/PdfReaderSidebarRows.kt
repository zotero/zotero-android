package org.zotero.android.pdf.reader.sidebar

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import org.zotero.android.pdf.data.Annotation
import org.zotero.android.pdf.reader.PdfReaderViewModel
import org.zotero.android.pdf.reader.PdfReaderViewState

@Composable
internal fun SidebarImageRow(
    viewModel: PdfReaderViewModel,
    viewState: PdfReaderViewState,
    annotation: Annotation,
    loadPreview: () -> Bitmap?,
    focusRequester: FocusRequester,
) {
    SidebarImageSection(
        loadPreview = loadPreview,
        viewModel = viewModel
    )
    SidebarDivider()
    SidebarTagsAndCommentsSection(
        annotation = annotation,
        viewModel = viewModel,
        viewState = viewState,
        focusRequester = focusRequester,
        shouldAddTopPadding = true,
    )
}

@Composable
internal fun SidebarInkRow(
    viewModel: PdfReaderViewModel,
    viewState: PdfReaderViewState,
    annotation: Annotation,
    loadPreview: () -> Bitmap?,
) {
    SidebarImageSection(loadPreview, viewModel)
    SidebarTagsSection(viewModel = viewModel, viewState = viewState, annotation = annotation)
}

@Composable
internal fun SidebarNoteRow(
    annotation: Annotation,
    viewModel: PdfReaderViewModel,
    viewState: PdfReaderViewState,
    focusRequester: FocusRequester,
) {
    SidebarTagsAndCommentsSection(
        annotation = annotation,
        viewModel = viewModel,
        viewState = viewState,
        focusRequester = focusRequester,
        shouldAddTopPadding = true,
    )
}

@Composable
internal fun SidebarHighlightRow(
    annotation: Annotation,
    viewModel: PdfReaderViewModel,
    viewState: PdfReaderViewState,
    annotationColor: Color,
    focusRequester: FocusRequester,
) {
    SidebarHighlightedTextSection(annotationColor = annotationColor, annotation = annotation)

    SidebarTagsAndCommentsSection(
        annotation = annotation,
        viewModel = viewModel,
        viewState = viewState,
        focusRequester = focusRequester,
        shouldAddTopPadding = false,
    )
}
