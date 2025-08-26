package org.zotero.android.pdf.annotationmore.rows

import PdfAnnotationMoreColorPicker
import PdfAnnotationMoreSizeSelector
import androidx.compose.runtime.Composable
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState
import org.zotero.android.screens.settings.elements.NewSettingsDivider

@Composable
internal fun PdfAnnotationMoreInkRow(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel,
) {
    PdfAnnotationMoreColorPicker(viewState, viewModel)
    NewSettingsDivider()
    PdfAnnotationMoreSizeSelector(
        viewState = viewState,
        viewModel = viewModel,
    )
}