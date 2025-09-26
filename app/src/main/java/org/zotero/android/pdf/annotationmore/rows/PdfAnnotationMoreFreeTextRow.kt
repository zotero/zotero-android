package org.zotero.android.pdf.annotationmore.rows

import PdfAnnotationMoreColorPicker
import PdfAnnotationMoreFontSizeSelector
import androidx.compose.runtime.Composable
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState
import org.zotero.android.screens.settings.elements.NewSettingsDivider

@Composable
internal fun PdfAnnotationMoreFreeTextRow(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel,
) {
    PdfAnnotationMoreFontSizeSelector(
        viewState = viewState,
        viewModel = viewModel,
    )
    NewSettingsDivider()
    PdfAnnotationMoreColorPicker(viewState, viewModel)
}