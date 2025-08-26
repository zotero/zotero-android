package org.zotero.android.pdf.annotationmore.rows

import PdfAnnotationMoreColorPicker
import PdfAnnotationMoreUnderlineText
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState
import org.zotero.android.screens.settings.elements.NewSettingsDivider

@Composable
internal fun PdfAnnotationMoreUnderlineRow(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel,
) {
    val annotationColor =
        Color(viewState.color.toColorInt())
    PdfAnnotationMoreUnderlineText(
        annotationColor = annotationColor,
        viewState = viewState,
        onValueChange = viewModel::onUnderlineTextValueChange,
    )
    NewSettingsDivider()
    PdfAnnotationMoreColorPicker(viewState, viewModel)
}