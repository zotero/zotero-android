package org.zotero.android.pdf.reader.pdfsearch.popup

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

@Composable
internal fun pdfReaderSearchPopupPositionProvider() = object : PopupPositionProvider {
    val localDensity = LocalDensity.current
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val extraXOffset = with(localDensity) {
            12.dp.toPx()
        }.toInt()
        val extraYOffset = with(localDensity) {
            8.dp.toPx()
        }.toInt()

        val xOffset = windowSize.width - popupContentSize.width - extraXOffset
        val yOffset = anchorBounds.bottom + extraYOffset

        return IntOffset(
            x = xOffset,
            y = yOffset
        )
    }
}