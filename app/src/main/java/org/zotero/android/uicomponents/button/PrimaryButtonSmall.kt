package org.zotero.android.uicomponents.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.theme.CustomTheme

/**
 * This is the Compose component for primary small colored button.
 * [backgroundColor] and [contentColor] are usually provided by the home theme,
 * but it's possible to set any value here.
 *
 * The button also shows progress bar instead of the text if [isLoading] is set
 * to true. It also disables the click callback without visually disabling the
 * button (design requirement).
 */
@Composable
fun PrimaryButtonSmall(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = CustomTheme.colors.dynamicTheme.primaryColor,
    contentColor: Color = CustomTheme.colors.dynamicTheme.buttonTextColor,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier
            .height(32.dp)
            .widthIn(min = 72.dp),
        enabled = isEnabled,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
        shape = CustomTheme.shapes.buttonSmall,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = CustomTheme.colors.disabledButtonBackground,
            disabledContentColor = CustomTheme.colors.disabledButtonContent
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                SmallButtonLoadingIndicator(contentColor)
            }
            Text(
                // Using alpha here instead of hiding the text to keep the width on loading
                modifier = Modifier.alpha(if (isLoading) 0f else 1f),
                text = text,
                style = CustomTheme.typography.h6
            )
        }
    }
}

@Composable
@Preview
private fun PrimaryButtonSmallPreview() {
    CustomTheme(isDarkTheme = true) {
        Column(
            modifier = Modifier
                .background(color = CustomTheme.colors.surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrimaryButtonSmall(
                text = "Just a button",
                onClick = {},
                modifier = Modifier
            )
            PrimaryButtonSmall(
                text = "Disabled button",
                onClick = {},
                modifier = Modifier.padding(top = 16.dp),
                isEnabled = false
            )
            PrimaryButtonSmall(
                text = "Just a button",
                onClick = {},
                modifier = Modifier.padding(top = 16.dp),
                isLoading = true
            )
        }
    }
}
