package org.zotero.android.pdf.reader.share

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun PdfReaderSharePopupOptionRow(
    text: String,
    @DrawableRes resIcon: Int,
    onOptionClick: (() -> Unit)? = null,
) {
    val color =
        MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(48.dp)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onOptionClick,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier.weight(1f),
            color = color,
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
        val iconSize = 20.dp
        Icon(
            modifier = Modifier.size(iconSize),
            painter = painterResource(resIcon),
            contentDescription = null,
            tint = color,
        )

        Spacer(modifier = Modifier.width(10.dp))
    }
}