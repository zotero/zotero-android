package org.zotero.android.uicomponents.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal object CustomRippleTheme : RippleTheme {

    @Composable
    override fun defaultColor(): Color = CustomTheme.colors.primaryContent

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleTheme.defaultRippleAlpha(
        CustomTheme.colors.primaryContent,
        lightTheme = !isSystemInDarkTheme()
    )
}

object HomeRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor(): Color = CustomTheme.colors.dynamicTheme.primaryColor

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(
        draggedAlpha = 0.25f,
        focusedAlpha = 0.25f,
        hoveredAlpha = 0.25f,
        pressedAlpha = 0.25f
    )
}

object PrimaryBackgroundRippleTheme : RippleTheme {

    @Composable
    override fun defaultColor(): Color = CustomTheme.colors.dynamicTheme.buttonTextColor

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(
        draggedAlpha = 0.25f,
        focusedAlpha = 0.25f,
        hoveredAlpha = 0.25f,
        pressedAlpha = 0.25f
    )
}
