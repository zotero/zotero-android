package org.zotero.android.uicomponents.button

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.Text
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.theme.CustomTheme

/**
 * This is the Compose component for primary colored button. [backgroundColor]
 * and [contentColor] are usually provided by the color theme, but it's possible
 * to set any value here.
 *
 * The button also shows progress bar instead of the text if [isLoading] is set
 * to true. It also disables the click callback without visually disabling the
 * button (design requirement).
 *
 * @param [isVisuallyDisabled] If true the button would act look like a disabled
 * button but would still propagate onclick events.
 *
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = CustomTheme.colors.zoteroDefaultBlue,
    contentColor: Color = CustomTheme.colors.dynamicTheme.buttonTextColor,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    isVisuallyDisabled: Boolean = false,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(
            rippleAlpha = RippleAlpha(
                draggedAlpha = 0.25f,
                focusedAlpha = 0.25f,
                hoveredAlpha = 0.25f,
                pressedAlpha = 0.25f
            ), color = CustomTheme.colors.dynamicTheme.buttonTextColor
        )
    ) {
        val disabledButtonBackground = CustomTheme.colors.disabledButtonBackground
        val disabledContentColor = CustomTheme.colors.disabledButtonContent
        Button(
            onClick = { if (!isLoading) onClick() },
            modifier = modifier.heightIn(min = 48.dp),
            enabled = isEnabled,
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
            shape = CustomTheme.shapes.button,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (!isVisuallyDisabled) backgroundColor else disabledButtonBackground,
                contentColor = if (!isVisuallyDisabled) contentColor else disabledContentColor,
                disabledBackgroundColor = disabledButtonBackground,
                disabledContentColor = disabledContentColor
            )
        ) {
            if (isLoading) {
                ButtonLoadingIndicator(contentColor)
            } else {
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
private fun PrimaryButtonPreview() {
    CustomTheme(isDarkTheme = true) {
        Column {
            PrimaryButton(
                text = "JUST A BUTTON",
                onClick = {},
                modifier = Modifier.width(200.dp)
            )
            PrimaryButton(
                text = "Disabled button",
                onClick = {},
                modifier = Modifier
                    .width(200.dp)
                    .padding(top = 16.dp),
                isEnabled = false
            )
            PrimaryButton(
                text = "Visually disabled button",
                onClick = {},
                modifier = Modifier
                    .width(200.dp)
                    .padding(top = 16.dp),
                isEnabled = true,
                isVisuallyDisabled = true
            )
            PrimaryButton(
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
