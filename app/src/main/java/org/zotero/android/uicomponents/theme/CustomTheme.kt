package org.zotero.android.uicomponents.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.zotero.android.uicomponents.CustomUriHandler

object CustomTheme {
    val colors
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomColors.current

    val typography
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomTypography.current

    val shapes
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomShapes.current
}

@Composable
fun CustomTheme(
    dynamicThemeColors: DynamicThemeColors = DynamicThemeColors(),
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val customTypography = CustomTypography()
    val customColors = createSemanticColors(
        dynamicThemeColors = dynamicThemeColors,
        isDarkTheme = isDarkTheme
    )

    CompositionLocalProvider(
        LocalCustomTypography provides customTypography,
        LocalTextStyle provides customTypography.default,
        LocalCustomColors provides customColors,
        LocalContentColor provides customColors.primaryContent,
        LocalRippleConfiguration provides CustomRippleTheme.createCustomRippleTheme(),
        LocalCustomShapes provides CustomShapes(),
        LocalUriHandler provides CustomUriHandler(LocalContext.current),
    ) {
        content()
    }
}

@Composable
fun CustomThemeWithStatusAndNavBars(
    dynamicThemeColors: DynamicThemeColors = DynamicThemeColors(),
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    statusBarBackgroundColor: Color = CustomTheme.colors.surface,
    navBarBackgroundColor: Color = CustomTheme.colors.surface,
    content: @Composable () -> Unit
) {
    val customTypography = CustomTypography()
    val customColors = createSemanticColors(
        dynamicThemeColors = dynamicThemeColors,
        isDarkTheme = isDarkTheme
    )

    CompositionLocalProvider(
        LocalCustomTypography provides customTypography,
        LocalTextStyle provides customTypography.default,
        LocalCustomColors provides customColors,
        LocalContentColor provides customColors.primaryContent,
        LocalRippleConfiguration provides CustomRippleTheme.createCustomRippleTheme(),
        LocalCustomShapes provides CustomShapes(),
        LocalUriHandler provides CustomUriHandler(LocalContext.current),
    ) {
        val isLight = CustomTheme.colors.isLight
        val systemUiController = rememberSystemUiController()
        SideEffect {
            systemUiController.setStatusBarColor(
                color = statusBarBackgroundColor,
                darkIcons = isLight
            )
            systemUiController.setNavigationBarColor(
                color = navBarBackgroundColor,
                darkIcons = isLight
            )
        }

        content()
    }

}
