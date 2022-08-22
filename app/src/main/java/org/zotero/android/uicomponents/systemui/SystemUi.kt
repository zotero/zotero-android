package org.zotero.android.uicomponents.systemui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun SolidStatusBar() {
    val systemUiController = rememberSystemUiController()
    val color = CustomTheme.colors.surface
    val isLight = CustomTheme.colors.isLight

    SideEffect {
        systemUiController.setStatusBarColor(
            color = color,
            darkIcons = isLight
        )
    }
}

@Composable
fun TransparentStatusBar() {
    val systemUiController = rememberSystemUiController()
    val isLight = CustomTheme.colors.isLight

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = isLight
        )
    }
}
