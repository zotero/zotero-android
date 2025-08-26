package org.zotero.android.architecture.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun BoxScope.DebugStopButton(isVisible: Boolean, onClick: () -> Unit) {
    if (isVisible) {
        val color = MaterialTheme.colorScheme.primary
        Canvas(modifier = Modifier
            .align(Alignment.BottomStart)
            .windowInsetsPadding(BottomAppBarDefaults.windowInsets)
            .padding(bottom = BottomAppBarDefaults.FlexibleBottomAppBarHeight)
            .padding(bottom = 16.dp)
            .padding(start = 44.dp)
            .size(56.dp)
            .safeClickable(onClick = onClick), onDraw = {
            drawCircle(color = color)
            drawRect(
                topLeft = Offset(x = 23.dp.toPx(), y = 23.dp.toPx()),
                size = Size(width = 11.dp.toPx(), height = 11.dp.toPx()),
                color = Color.White
            )
        })
    }
}