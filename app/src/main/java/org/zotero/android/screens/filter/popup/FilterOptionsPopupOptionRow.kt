package org.zotero.android.screens.filter.popup

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun FilterOptionsPopupOptionRow(
    isEnabled: Boolean = true,
    textAndIconColor: Color? = null,
    text: String,
    @DrawableRes resIcon: Int? = null,
    onOptionClick: (() -> Unit)? = null,
) {
    val color = if (isEnabled) {
        textAndIconColor ?: MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFF89898C)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .heightIn(min = 48.dp)
            .safeClickable(
                enabled = isEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onOptionClick,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        val iconSize = 24.dp
        if (resIcon != null) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(resIcon),
                contentDescription = null,
                tint = color,
            )
        } else {
            Spacer(modifier = Modifier.width(iconSize))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            modifier = Modifier.padding(end = 16.dp),
            color = color,
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}