package org.zotero.android.screens.htmlepub.reader.web.actionmenu

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.google.gson.JsonArray
import com.pspdfkit.internal.utilities.dpToPx

@Composable
internal fun htmlEpubActionMenuPopupPositionProvider(
    selectedTextParamsRects: JsonArray,
) = object : PopupPositionProvider {
    val localDensity = LocalDensity.current
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val topLeftX = selectedTextParamsRects[0].asString.toDouble()
        val bottomRightX = selectedTextParamsRects[2].asString.toDouble()

        val textSelectionMiddleX = topLeftX + (bottomRightX - topLeftX) / 2.0
        val textSelectionTopY = selectedTextParamsRects[1].asString.toDouble()

        val popupHalfWidth = popupContentSize.width / 6
        val finalX = textSelectionMiddleX - popupHalfWidth
        val finalY = textSelectionTopY + popupContentSize.height / 8

        return IntOffset(
            x = finalX.dp.dpToPx(localDensity).toInt(),
            y = finalY.dp.dpToPx(localDensity).toInt()
        )
    }
}
