package org.zotero.android.pdf.annotationmore.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.zotero.android.pdf.annotationmore.MoreColorPicker
import org.zotero.android.pdf.annotationmore.MoreFontSizeSelector
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState
import org.zotero.android.pdf.annotationmore.SpacerDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfAnnotationMoreFreeTextRow(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = CustomTheme.colors.zoteroEditFieldBackground)
    ) {
        SpacerDivider()
        MoreFontSizeSelector(
            viewState = viewState,
            viewModel = viewModel,
        )
        SpacerDivider()
        MoreColorPicker(viewState, viewModel)

    }
}