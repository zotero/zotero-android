package org.zotero.android.pdf.annotationmore.rows

import PdfAnnotationMoreColorPicker
import PdfAnnotationMoreHighlightText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState
import org.zotero.android.screens.settings.elements.NewSettingsDivider

@Composable
internal fun PdfAnnotationMoreHighlightRow(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel,
) {
    val annotationColor =
        Color(viewState.color.toColorInt())
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        PdfAnnotationMoreHighlightText(
            annotationColor = annotationColor,
            viewState = viewState,
            onValueChange = viewModel::onHighlightTextValueChange,
        )
        NewSettingsDivider()
        PdfAnnotationMoreColorPicker(viewState, viewModel)
    }
}