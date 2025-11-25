package org.zotero.android.pdf.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.misc.NewDivider

@Composable
internal fun BoxScope.PdfReaderBottomPanel(
    layoutType: CustomLayoutSize.LayoutType,
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(layoutType.calculateAllItemsBottomPanelHeight())
            .align(Alignment.BottomStart)
            .background(color = MaterialTheme.colorScheme.surface)
    ) {
        NewDivider(modifier = Modifier.align(Alignment.TopStart))
        val filterDrawable =
            if (viewState.filter == null) {
                Drawables.filter_list_off_24px
            } else {
                Drawables.filter_list_24px
            }

        TooltipBox(
            modifier = Modifier
                .padding(start = 24.dp)
                .padding(top = 4.dp),
            positionProvider = rememberTooltipPositionProvider(
                TooltipAnchorPosition.Above,
                0.dp
            ),
            tooltip = {
                PlainTooltip(
                    modifier = Modifier
                        .padding(start = 12.dp),
                ) {
                    Text(
                        stringResource(
                            Strings.all_items_bottom_panel_filters
                        )
                    )
                }
            },
            state = rememberTooltipState()
        ) {
            IconWithPadding(
                drawableRes = filterDrawable,
                onClick = vMInterface::showFilterPopup
            )

        }

    }
}
