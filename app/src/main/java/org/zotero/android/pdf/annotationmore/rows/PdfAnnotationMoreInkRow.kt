package org.zotero.android.pdf.annotationmore.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.annotationmore.MoreColorPicker
import org.zotero.android.pdf.annotationmore.MoreSidebarDivider
import org.zotero.android.pdf.annotationmore.MoreSizeSelector
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewModel
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreViewState
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfAnnotationMoreInkRow(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel,
    layoutType: CustomLayoutSize.LayoutType,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = CustomTheme.colors.zoteroEditFieldBackground)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        MoreColorPicker(viewState, viewModel)
        Spacer(modifier = Modifier.height(4.dp))
        MoreSidebarDivider(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .padding(start = 16.dp)
        )
        MoreSizeSelector(
            viewState = viewState,
            viewModel = viewModel,
            layoutType = layoutType
        )
    }
}