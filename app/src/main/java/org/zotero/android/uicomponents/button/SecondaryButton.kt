package org.zotero.android.uicomponents.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.theme.CustomTheme

/**
 * This is the Compose component for secondary (outlined) button.
 * [contentColor] represents active border and text color and is usually provided
 * by the dynamic theme.
 * [iconPainter] is an optional icon to draw before the text.
 *
 * The button also shows progress bar instead of the text if [isLoading] is set
 * to true. It also disables the click callback without visually disabling the
 * button (design requirement).
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconPainter: Painter? = null,
    contentColor: Color = CustomTheme.colors.primaryContent,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
) {
    OutlinedButton(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier.heightIn(min = 48.dp),
        enabled = isEnabled,
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
        shape = CustomTheme.shapes.button,
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isEnabled) contentColor else CustomTheme.colors.disabledButtonBackground
        ),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = contentColor,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = CustomTheme.colors.disabledButtonContent
        )
    ) {
        if (isLoading) {
            ButtonLoadingIndicator(contentColor)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconPainter != null) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = text,
                    style = CustomTheme.typography.h2,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
@Preview
private fun SecondaryButtonPreview() {
    CustomTheme(isDarkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            SecondaryButton(
                text = "Just a button",
                onClick = {},
                modifier = Modifier.width(200.dp)
            )
            SecondaryButton(
                text = "Button with icon",
                onClick = {},
                modifier = Modifier
                    .width(200.dp)
                    .padding(top = 16.dp),
                iconPainter = painterResource(id = Drawables.ic_delete_20dp)
            )
            SecondaryButton(
                text = "Disabled button",
                onClick = {},
                modifier = Modifier
                    .width(200.dp)
                    .padding(top = 16.dp),
                isEnabled = false
            )
            SecondaryButton(
                text = "Loading button",
                onClick = {},
                modifier = Modifier
                    .width(200.dp)
                    .padding(top = 16.dp),
                isLoading = true
            )
        }
    }
}
