package org.zotero.android.uicomponents.reorder

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

fun Modifier.draggedItem(offset: Float?): Modifier {
    return zIndex(offset?.let { 1f } ?: 0f)
        .graphicsLayer {
            translationY = offset ?: 0f
        }
}
