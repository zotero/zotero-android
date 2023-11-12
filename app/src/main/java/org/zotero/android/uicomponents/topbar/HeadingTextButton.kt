package org.zotero.android.uicomponents.topbar

import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun HeadingTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = CustomTheme.colors.zoteroDefaultBlue,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
) {
    IconButton(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier,
    ) {
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
                style = CustomTheme.typography.default
            )
        }
    }
}
