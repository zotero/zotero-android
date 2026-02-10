package org.zotero.android.screens.htmlepub.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.htmlepub.reader.data.AnnotationTool
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderTool
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun HtmlEpubReaderAnnotationCreationToggleButton(
    activeAnnotationTool: AnnotationTool?,
    pdfReaderTool: HtmlEpubReaderTool,
    toggleButton: (AnnotationTool) -> Unit
) {
    val isSelected = activeAnnotationTool == pdfReaderTool.type
    val tintColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val roundCornerShape = CircleShape
    var modifier = Modifier
        .padding(horizontal = 4.dp)
        .size(40.dp)
        .clip(roundCornerShape)
        .safeClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { toggleButton(pdfReaderTool.type) },
        )
    if (isSelected) {
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.primary,
            shape = roundCornerShape
        )
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(id = pdfReaderTool.image),
            contentDescription = null,
            tint = tintColor
        )

    }
}

