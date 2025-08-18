package org.zotero.android.uicomponents.theme

import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable

object CustomRippleTheme {
    @Composable
    fun createCustomRippleTheme() = RippleConfiguration(color = CustomTheme.colors.primaryContent
    )
}
