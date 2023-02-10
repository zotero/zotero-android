package org.zotero.android.uicomponents.controls

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionItem
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionRow
import org.zotero.android.uicomponents.theme.CustomPalette

@Composable
fun LongPressOptionsPopup(
    optionsItems: List<LongPressOptionItem>,
    onOptionClick: (LongPressOptionItem) -> Unit,
    onDismissPopup: () -> Unit,
) {
    val localDensity = LocalDensity.current
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = onDismissPopup,
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val popupOffsetX = with(localDensity) {
                     12.dp.toPx()
                }
                val popupOffsetY = with(localDensity) {
                    24.dp.toPx()
                }

                return IntOffset(
                    x = popupOffsetX.toInt(),
                    y = anchorBounds.top - popupOffsetY.toInt()
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .clip(shape = RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = CustomPalette.Black,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            optionsItems.forEach { mention ->
                LongPressOptionRow(
                    optionItem = mention,
                    onOptionClick = onOptionClick
                )
            }
        }
    }
}

