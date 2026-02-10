package org.zotero.android.screens.htmlepub.toolbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun HtmlEpubReaderFilterCircle(hex: String, onClick: () -> Unit) {
    val color = hex.toColorInt()
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(20.dp)
                .safeClickable(
                    onClick = onClick, interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ), onDraw = {
                drawCircle(color = Color(color))
            })
    }

}