package org.zotero.android.pdf.reader.toolbar

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun PdfReaderAnnotationCreationButton(
    isEnabled: Boolean,
    iconInt: Int,
    onButtonClick: (() -> Unit)? = null
) {
    val tintColor = if (isEnabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val modifier = Modifier
        .padding(horizontal = 4.dp)
        .size(40.dp)
        .safeClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onButtonClick,
            enabled = isEnabled
        )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(id = iconInt),
            contentDescription = null,
            tint = tintColor
        )
    }

}
