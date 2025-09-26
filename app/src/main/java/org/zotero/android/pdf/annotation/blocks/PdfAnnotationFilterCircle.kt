package org.zotero.android.pdf.annotation.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import org.zotero.android.uicomponents.foundation.debounceClickable
import org.zotero.android.uicomponents.theme.CustomPalette

@Composable
internal fun PdfAnnotationFilterCircle(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = hex.toColorInt()
    Canvas(modifier = Modifier
        .size(28.dp)
        .debounceClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ), onDraw = {
        drawCircle(color = Color(color))
        if (isSelected) {
            drawCircle(
                color = CustomPalette.White,
                radius = 11.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    })
}
