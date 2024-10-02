package org.zotero.android.pdf.reader.pdfsearch

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfReaderSearchPopup(
    viewState: PdfReaderViewState,
    viewModel: PdfReaderVMInterface,
) {
    val backgroundColor = CustomTheme.colors.cardBackground
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            focusable = true
        ),
        onDismissRequest = viewModel::hidePdfSearch,
        popupPositionProvider = createPdfReaderSearchPopupPositionProvider(),

    ) {
        CustomScaffold(
            modifier = Modifier
                .width(350.dp)
                .height(530.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                ),
            backgroundColor = backgroundColor,
        ) {
            PdfReaderSearchScreen(onBack = viewModel::hidePdfSearch)
        }
    }
}

@Composable
private fun createPdfReaderSearchPopupPositionProvider() = object : PopupPositionProvider {
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