package org.zotero.android.uicomponents.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun DynamicTheme(
    dynamicThemeColors: DynamicThemeColors = DynamicThemeColors(),
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = createSemanticColors(
        dynamicThemeColors = dynamicThemeColors,
        isDarkTheme = isDarkTheme
    )

    CompositionLocalProvider(
        LocalCustomColors provides colors,
        LocalContentColor provides colors.primaryContent,
    ) {
        content()
    }
}
