package org.zotero.android.pdf.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun BoxScope.PdfReaderBottomPanel(
    layoutType: CustomLayoutSize.LayoutType,
    viewModel: PdfReaderViewModel,
    viewState: PdfReaderViewState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(layoutType.calculateAllItemsBottomPanelHeight())
            .align(Alignment.BottomStart)
            .background(color = CustomTheme.colors.surface)
    ) {
        CustomDivider(modifier = Modifier.align(Alignment.TopStart))
        val filterDrawable =
            if (viewState.filter == null) {
                Drawables.filter_list_off_24px
            } else {
                Drawables.filter_list_24px
            }
        IconWithPadding(
            modifier = Modifier
                .padding(start = 24.dp)
                .align(Alignment.CenterStart),
            drawableRes = filterDrawable,
            onClick = viewModel::showFilterPopup
        )
    }
}
