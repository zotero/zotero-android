package org.zotero.android.pdf.annotation.row

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.annotation.ColorPicker
import org.zotero.android.pdf.annotation.FontSizeSelector
import org.zotero.android.pdf.annotation.PdfAnnotationViewModel
import org.zotero.android.pdf.annotation.PdfAnnotationViewState
import org.zotero.android.pdf.annotation.TagsSection
import org.zotero.android.pdf.annotationmore.SpacerDivider
import org.zotero.android.pdf.data.PDFAnnotation

@Composable
internal fun PdfAnnotationTextRow(
    annotation: PDFAnnotation,
    viewState: PdfAnnotationViewState,
    viewModel: PdfAnnotationViewModel,
    layoutType: CustomLayoutSize.LayoutType,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 8.dp)
    ) {
        if (annotation.isZoteroAnnotation) {
            SpacerDivider()
            FontSizeSelector(
                fontSize = viewState.fontSize,
                onFontSizeDecrease = viewModel::onFontSizeDecrease,
                onFontSizeIncrease = viewModel::onFontSizeIncrease,
            )
            SpacerDivider()
            ColorPicker(viewState, viewModel)
            SpacerDivider()
            Spacer(modifier = Modifier.height(4.dp))
            TagsSection(
                viewModel = viewModel,
                viewState = viewState,
                layoutType = layoutType
            )
            Spacer(modifier = Modifier.height(4.dp))
            SpacerDivider()
        }
    }
}

