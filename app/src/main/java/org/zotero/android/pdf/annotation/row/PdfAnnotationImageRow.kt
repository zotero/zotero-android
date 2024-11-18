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
import org.zotero.android.pdf.annotation.CommentSection
import org.zotero.android.pdf.annotation.PdfAnnotationViewModel
import org.zotero.android.pdf.annotation.PdfAnnotationViewState
import org.zotero.android.pdf.annotation.TagsSection
import org.zotero.android.pdf.reader.sidebar.SidebarDivider

@Composable
internal fun PdfAnnotationImageRow(
    viewState: PdfAnnotationViewState,
    viewModel: PdfAnnotationViewModel,
    layoutType: CustomLayoutSize.LayoutType,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 8.dp)
    ) {
        CommentSection(viewState, layoutType, viewModel)
        Spacer(modifier = Modifier.height(4.dp))
        SidebarDivider(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        ColorPicker(viewState, viewModel)
        Spacer(modifier = Modifier.height(4.dp))
        SidebarDivider(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        TagsSection(viewModel = viewModel, viewState = viewState, layoutType = layoutType)
    }
}
