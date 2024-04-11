package org.zotero.android.uicomponents.topbar

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun NewHeadingTextButton(
    text: String,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    contentColor: Color = CustomTheme.colors.zoteroDefaultBlue,
    style: TextStyle = CustomTheme.typography.default,
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .safeClickable(
                onClick = {
                    if (!isLoading) onClick()
                },
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = if (isEnabled) contentColor else CustomTheme.colors.disabledContent,
                style = style
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
    }
}
