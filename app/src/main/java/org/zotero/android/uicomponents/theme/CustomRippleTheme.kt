package org.zotero.android.uicomponents.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.RippleDefaults
import androidx.compose.runtime.Composable

object CustomRippleTheme {
    @Composable
    fun createCustomRippleTheme() = RippleConfiguration(
        rippleAlpha = RippleDefaults.rippleAlpha(
            CustomTheme.colors.primaryContent,
            lightTheme = !isSystemInDarkTheme()
        ), color = CustomTheme.colors.primaryContent
    )
}
