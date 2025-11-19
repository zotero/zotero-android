package org.zotero.android.screens.citbibexport

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.screens.settings.csllocalepicker.SettingsCslLocalePickerScreen
import org.zotero.android.screens.settings.stylepicker.SettingsStylePickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import java.io.File

@Composable
internal fun CitBibExportNavigation(onExportHtml: (file: File) -> Unit) {
    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    ZoteroNavHost(
        navController = navController,
        startDestination = CibBibExportDestinations.CIT_BIB_EXPORT,
    ) {
        citBibExportNavScreens(navigation = navigation, onExportHtml = onExportHtml)
    }
}

internal fun NavGraphBuilder.citBibExportNavScreens(
    navigation: ZoteroNavigation,
    onExportHtml: (file: File) -> Unit,
) {
    cibBibExportScreen(
        onBack = navigation::onBack,
        toStylePicker = navigation::toStylePicker,
        toLocalePicker = navigation::toCslLocalePicker,
        onExportHtml = onExportHtml,
    )
    stylePickerScreen(
        onBack = navigation::onBack,
    )
    cslLocalePickerScreen(onBack = navigation::onBack)
}

fun NavGraphBuilder.cibBibExportScreen(
    onBack: () -> Unit,
    toStylePicker: () -> Unit,
    toLocalePicker: () -> Unit,
    onExportHtml: (file: File) -> Unit,
) {
    composable(
        route = CibBibExportDestinations.CIT_BIB_EXPORT,
    ) {
        CitBibExportScreen(
            onBack = onBack,
            navigateToStylePicker = toStylePicker,
            navigateToCslLocalePicker = toLocalePicker,
            onExportHtml = onExportHtml,
        )
    }
}

private fun NavGraphBuilder.stylePickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = CibBibExportDestinations.STYLE_PICKER,
    ) {
        SettingsStylePickerScreen(
            onBack = onBack,
        )
    }
}

private fun NavGraphBuilder.cslLocalePickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = CibBibExportDestinations.CSL_LOCALE_PICKER,
    ) {
        SettingsCslLocalePickerScreen(
            onBack = onBack,
        )
    }
}

private object CibBibExportDestinations {
    const val CIT_BIB_EXPORT = "citBibExport"
    const val CSL_LOCALE_PICKER = "cslLocalPicker"
    const val STYLE_PICKER = "stylePicker"
}


fun ZoteroNavigation.toCitBibExport() {
    navController.navigate(CibBibExportDestinations.CIT_BIB_EXPORT)
}

fun ZoteroNavigation.toStylePicker() {
    navController.navigate(CibBibExportDestinations.STYLE_PICKER)
}

fun ZoteroNavigation.toCslLocalePicker() {
    navController.navigate(CibBibExportDestinations.CSL_LOCALE_PICKER)
}