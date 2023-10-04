package org.zotero.android.pdf.reader

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable
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
    ) {
        SidebarDivider(modifier = Modifier.align(Alignment.TopStart))
        val filterDrawable =
            if (viewState.filter == null) {
                Drawables.filter_icon_unselected
            } else {
                Drawables.filter_icon_selected
            }
        Icon(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(layoutType.calculatePdfSidebarBottomPanelIconSize())
                .align(Alignment.CenterStart)
                .safeClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(),
                    onClick = {
                        viewModel.showFilterPopup()
                    }
                ),
            painter = painterResource(id = filterDrawable),
            contentDescription = null,
            tint = CustomTheme.colors.zoteroBlueWithDarkMode
        )
    }
}
