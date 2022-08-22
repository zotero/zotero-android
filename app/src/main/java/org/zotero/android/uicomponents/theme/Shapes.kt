@file:Suppress("MagicNumber")

package org.zotero.android.uicomponents.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val LocalCustomShapes: ProvidableCompositionLocal<CustomShapes> =
    staticCompositionLocalOf { CustomShapes() }

data class CustomShapes(
    val bottomSheet: Shape = RoundedCornerShape(
        topStart = BOTTOM_SHEET_CORNER_RADIUS,
        topEnd = BOTTOM_SHEET_CORNER_RADIUS
    ),
    val button: Shape = RoundedCornerShape(28.dp),
    val buttonSmall: Shape = RoundedCornerShape(16.dp),
) {

    companion object {
        val BOTTOM_SHEET_CORNER_RADIUS = 24.dp
    }
}
