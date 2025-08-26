package org.zotero.android.screens.downloadedfiles

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import org.zotero.android.architecture.ui.CustomLayoutSize

@Composable
internal fun downloadedFilesPopupPositionProvider() = object : PopupPositionProvider {
    val localDensity = LocalDensity.current
    val isTablet = CustomLayoutSize.calculateLayoutType().isTablet()
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val extraXOffset = with(localDensity) {
            -14.dp.toPx()
        }.toInt()
        val extraYOffset = with(localDensity) {
            2.dp.toPx()
        }.toInt()

        val xOffset = if (isTablet) {
            anchorBounds.left + (anchorBounds.right - anchorBounds.left) - popupContentSize.width / 2 + extraXOffset
        } else {
            val q = windowSize.width - popupContentSize.width
            q - (windowSize.width - anchorBounds.right)
        }

        val yOffset = if (isTablet) {
            anchorBounds.top - popupContentSize.height - extraYOffset
        } else {
            anchorBounds.bottom + extraYOffset
        }
        return IntOffset(
            x = xOffset,
            y = yOffset
        )
    }
}