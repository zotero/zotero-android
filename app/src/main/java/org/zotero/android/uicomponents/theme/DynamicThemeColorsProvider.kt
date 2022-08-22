package org.zotero.android.uicomponents.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import org.zotero.android.androidx.content.isDarkTheme
import org.zotero.android.uicomponents.theme.CustomSemanticColors.DynamicTheme

/**
 * This interface provides current [DynamicThemeColors] in a reactive fashion.
 * If some source is changing the theme for the whole app (e.g. the home is
 * selected), this provider pushes an update to all consumers to update the UI.
 * If no home is selected (e.g. login activity), the theme defaults are used.
 */
interface DynamicThemeColorsProvider {
    val stream: Flow<DynamicThemeColors>
    val cachedValue: DynamicThemeColors
}

@Suppress("MagicNumber")
data class DynamicThemeColors(
    val light: DynamicTheme = DynamicTheme(
        primaryColor = Color(0xFF008CFF),
        shadeOne = Color(0xFF4DAEFF),
        shadeTwo = Color(0xFF90C7F2),
        shadeThree = Color(0xFFCCE8FF),
        shadeFour = Color(0xFFE6F4FF),
        highlightColor = Color(0xFF008CFF),
        buttonTextColor = Color(0xFFFFFFFF),
    ),
    val dark: DynamicTheme = DynamicTheme(
        primaryColor = Color(0xFF008CFF),
        shadeOne = Color(0xFF0070CC),
        shadeTwo = Color(0xFF005499),
        shadeThree = Color(0xFF003866),
        shadeFour = Color(0xFF001C33),
        highlightColor = Color(0xFF008CFF),
        buttonTextColor = Color(0xFFFFFFFF),
    ),
)

fun DynamicThemeColors.resolveTheme(context: Context): DynamicTheme {
    return if (context.isDarkTheme()) dark else light
}
