package org.zotero.android.screens.reader.web.actionmenu

import androidx.compose.material3.TopAppBarDefaults
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
import org.zotero.android.screens.reader.ReaderViewState

@Composable
internal fun readerActionMenuPopupPositionProvider(
    selectedTextParamsRects: JsonArray,
    viewState: ReaderViewState
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

        val popupHalfWidth = popupContentSize.width / 4
        val finalX = textSelectionMiddleX - popupHalfWidth

        var topBarExtraOffsetYPx = 0
        if (viewState.isPdfOrHtml() && !viewState.isTopBarVisible) {
            topBarExtraOffsetYPx = TopAppBarDefaults.TopAppBarExpandedHeight.dpToPx(localDensity).toInt() - 50
        }

        val finalY = textSelectionTopY + popupContentSize.height / 4 - topBarExtraOffsetYPx

        return IntOffset(
            x = finalX.dp.dpToPx(localDensity).toInt(),
            y = finalY.dp.dpToPx(localDensity).toInt()
        )
    }
}
