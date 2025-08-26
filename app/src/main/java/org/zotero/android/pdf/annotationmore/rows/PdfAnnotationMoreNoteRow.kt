package org.zotero.android.pdf.annotationmore.rows

import PdfAnnotationMoreColorPicker
import androidx.compose.runtime.Composable
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState

@Composable
internal fun PdfAnnotationMoreNoteRow(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel,
) {
    PdfAnnotationMoreColorPicker(viewState, viewModel)
}