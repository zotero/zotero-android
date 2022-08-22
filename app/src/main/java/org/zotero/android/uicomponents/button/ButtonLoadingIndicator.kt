package org.zotero.android.uicomponents.button

import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun ButtonLoadingIndicator(color: Color) {
    CircularProgressIndicator(
        modifier = Modifier.size(24.dp),
        color = color,
        strokeWidth = 2.dp,
    )
}

@Composable
internal fun SmallButtonLoadingIndicator(color: Color) {
    CircularProgressIndicator(
        modifier = Modifier.size(16.dp),
        color = color,
        strokeWidth = 1.5.dp,
    )
}
