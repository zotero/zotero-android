@file:Suppress("MagicNumber")

package org.zotero.android.uicomponents.theme

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalCustomColors: ProvidableCompositionLocal<CustomSemanticColors> =
    staticCompositionLocalOf { lightCustomColors(DynamicThemeColors()) }

object CustomPalette {
    val Green = Color(0xFF007f03)
    val Blue = Color(0xFF0000FF)
    val Black = Color(0xFF000000)
    val Charcoal = Color(0xFF2A2D35)
    val DarkCharcoal = Color(0xFF1A1C21)
    val LightCharcoal = Color(0xFF606272)
    val CoolGray = Color(0xFF888AA0)
    val LightCoolGray = Color(0xFFC3C4CF)
    val FeatherGray = Color(0xFFDEDFEB)
    val FogGray = Color(0xFFEEEFF5)
    val LightFogGray = Color(0xFFF5F5F9)
    val White = Color(0xFFFFFFFF)
    val ErrorRed = Color(0xFFE0244D)
    val ErrorRedDark = Color(0xFF2D070F)

    val ErrorRedDarkShadeThree = Color(0xFF5A0E1F)
    val ErrorRedLight = Color(0xFFF8E6EA)
    val ErrorRedLightShadeFour = Color(0xFFF8E6EA)
    val PendingYellow = Color(0xFFF2C94C)
    val zoteroItemDetailSectionTitle = Color(0xFF6C6C73)
    val pdfAnnotationSidebarSelectedItem = Color(0xFF3C59B2)

    val SystemGray = Color(0xFF8E8E93)
}

/**
 * [CustomSemanticColors] is a set of colors used across the app, named
 * according to their usage. It's useful for colors that are affected by
 * the theme and are used by different components in the same way.
 *
 * [cardBackground] is used for most cards in the app by default
 * [disabledButtonBackground] is used for background of disabled buttons.
 * [disabledButtonContent] is used for text and icons of disabled buttons.
 * [disabledContent] is used for interactive content elements which are disabled.
 * [dynamicTheme] provides the color theme of the app which may be changed dynamically
 * (selecting the home applies its theme to the whole app)
 * [error] is the color for error messages.
 * [inputBar] is used for search bars, link bars and similar input elements
 * [isLight] returns true if light theme is currently used, false for dark theme.
 * [primaryContent] is used for the main content text and icons.
 * [secondaryContent] is used for the secondary content text and icons
 * (description, optional etc.).
 * [surface] is used for all the surface backgrounds.
 * [uiControl] is used for close icons and up arrows
 * [windowBackground] is a color of completely naked background.
 * It is similar to window background in Android framework.
 */
data class CustomSemanticColors(
    val cardBackground: Color,
    val disabledButtonBackground: Color,
    val disabledButtonContent: Color,
    val disabledContent: Color,
    val divider: Color,
    val dynamicTheme: DynamicTheme,
    val error: Color,
    val errorSecondary: Color,
    val inputBar: Color,
    val isLight: Boolean,
    val primaryContent: Color,
    val scrim: Color,
    val secondaryContent: Color,
    val surface: Color,
    val uiControl: Color,
    val windowBackground: Color,
    val zoteroDefaultBlue:Color,
    val zoteroBlueWithDarkMode: Color,
    val zoteroItemDetailSectionBackground: Color,
    val zoteroEditFieldBackground: Color,
    val popupBackgroundTop: Color,
    val popupBackgroundContent: Color,
    val popupSelectedRow: Color,
    val sortPickerBackground: Color,
    val sortPickerSelected: Color,
    val sortPickerUnSelected: Color,
    val pdfAnnotationsItemBackground: Color,
    val pdfAnnotationsTopbarBackground: Color,
    val pdfAnnotationsFormBackground: Color,
    val pdfAnnotationsDividerBackground: Color,
    val pdfAnnotationsSearchBarBackground: Color,
    val libraryDividerBackground: Color,
    val addNoteBottomBar: Color,
    val reorderButtonColor: Color,
    val dialogBorderColor: Color,
    val allItemsRowTitleColor: Color,
    val pdfToolbarBackgroundColor: Color,
    val pdfSizePickerColor: Color,
    val allItemsInfoIconColor: Color,
    val topBarBackgroundColor: Color,
    val newDividerColor: Color,
) {

    data class DynamicTheme(
        val primaryColor: Color,
        val shadeOne: Color,
        val shadeTwo: Color,
        val shadeThree: Color,
        val shadeFour: Color,
        val highlightColor: Color,
        val buttonTextColor: Color,
    )
}

fun lightCustomColors(
    dynamicThemeColors: DynamicThemeColors,
) = CustomSemanticColors(
    cardBackground = CustomPalette.White,
    disabledButtonBackground = CustomPalette.LightCoolGray,
    disabledButtonContent = CustomPalette.White,
    disabledContent = CustomPalette.LightCoolGray,
    divider = CustomPalette.FogGray,
    dynamicTheme = dynamicThemeColors.light,
    error = CustomPalette.ErrorRed,
    errorSecondary = CustomPalette.ErrorRedLight,
    inputBar = CustomPalette.FogGray,
    isLight = true,
    primaryContent = CustomPalette.Charcoal,
    scrim = CustomPalette.Black.copy(alpha = 0.4f),
    secondaryContent = CustomPalette.CoolGray,
    surface = CustomPalette.White,
    uiControl = CustomPalette.Charcoal,
    windowBackground = CustomPalette.FogGray,
    zoteroBlueWithDarkMode = Color(0xFF4071E6),
    zoteroItemDetailSectionBackground = Color(0xFFF2F2F7),
    zoteroEditFieldBackground = Color(0xFFFFFFFF),
    popupBackgroundTop = Color(0xFFF8F8F8),
    popupBackgroundContent = Color(0xFFFFFFFF),
    popupSelectedRow = Color(0xFFD1D1D6),
    sortPickerBackground = Color(0xFFFFFFFF),
    sortPickerSelected = Color(0xFFFFFFFF),
    sortPickerUnSelected = Color(0xFFEEEEEF),
    pdfAnnotationsItemBackground = Color(0xFFFFFFFF),
    pdfAnnotationsTopbarBackground = Color(0xFFFAFAFD),
    pdfAnnotationsFormBackground = Color(0xFFF2F2F7),
    pdfAnnotationsDividerBackground = Color(0xFFE4E4E7),
    pdfAnnotationsSearchBarBackground = Color(0xFFE4E3EA),
    libraryDividerBackground = Color(0xFFE4E4E7),
    addNoteBottomBar = Color(0xFFF7F7F7),
    reorderButtonColor = Color(0xFFC5C5C7),
    dialogBorderColor = Color(0xFF69696F),
    allItemsRowTitleColor = Color.Black,
    pdfToolbarBackgroundColor= Color(0xFFFCFBFD),
    pdfSizePickerColor = Color(0xFF6E6D73),
    allItemsInfoIconColor = Color(0xFF1A88FF),
    zoteroDefaultBlue = Color(0xFF4072E5),
    topBarBackgroundColor = Color(0xFFF7F7F7),
    newDividerColor = Color(0xFFE5E5E5),
)

fun darkCustomColors(
    dynamicThemeColors: DynamicThemeColors,
) = CustomSemanticColors(
    cardBackground = CustomPalette.DarkCharcoal,
    disabledButtonBackground = CustomPalette.DarkCharcoal,
    disabledButtonContent = CustomPalette.LightCharcoal,
    disabledContent = CustomPalette.CoolGray,
    divider = CustomPalette.Charcoal,
    dynamicTheme = dynamicThemeColors.dark,
    error = CustomPalette.ErrorRed,
    errorSecondary = CustomPalette.ErrorRedDark,
    inputBar = CustomPalette.Charcoal,
    isLight = false,
    primaryContent = CustomPalette.White,
    scrim = Color(0xFF444557).copy(alpha = 0.4f),
    secondaryContent = CustomPalette.CoolGray,
    surface = CustomPalette.Black,
    uiControl = CustomPalette.CoolGray,
    windowBackground = CustomPalette.DarkCharcoal,
    zoteroBlueWithDarkMode = Color(0xFF335BB8),
    zoteroItemDetailSectionBackground = Color(0xFF1C1C1E),
    zoteroEditFieldBackground = Color(0xFF2C2C2E),
    popupBackgroundTop = Color(0xFF282828),
    popupBackgroundContent = Color(0xFF1C1C1E),
    popupSelectedRow = Color(0xFF3A3A3D),
    sortPickerBackground = Color(0xFF1C1C1E),
    sortPickerSelected = Color(0xFF69696F),
    sortPickerUnSelected = Color(0xFF313136),
    pdfAnnotationsItemBackground = Color(0xFF000000),
    pdfAnnotationsTopbarBackground = Color(0xFF282828),
    pdfAnnotationsFormBackground = Color(0xFF1C1C1E),
    pdfAnnotationsDividerBackground = Color(0xFF1B1B1C),
    pdfAnnotationsSearchBarBackground = CustomPalette.Charcoal,
    libraryDividerBackground = Color(0xFF1B1B1C),
    addNoteBottomBar = Color(0xFF121212),
    reorderButtonColor = Color(0xFF464649),
    dialogBorderColor = Color(0xFFFFFFFF),
    allItemsRowTitleColor = Color.White,
    pdfToolbarBackgroundColor= Color(0xFF282729),
    pdfSizePickerColor = Color(0xFF8E8E93),
    allItemsInfoIconColor = Color(0xFF0976E5),
    zoteroDefaultBlue = Color(0xFF4072E5),
    topBarBackgroundColor = Color(0xFF282928),
    newDividerColor = CustomPalette.Charcoal,

)

internal fun createSemanticColors(
    dynamicThemeColors: DynamicThemeColors,
    isDarkTheme: Boolean,
): CustomSemanticColors {
    return if (isDarkTheme) {
        darkCustomColors(dynamicThemeColors)
    } else {
        lightCustomColors(dynamicThemeColors)
    }
}
