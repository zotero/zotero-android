package org.zotero.android.screens.settings

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.screens.settings.account.SettingsAccountScreen
import org.zotero.android.screens.settings.cite.SettingsCiteScreen
import org.zotero.android.screens.settings.citesearch.SettingsCiteSearchScreen
import org.zotero.android.screens.settings.csllocalepicker.SettingsCslLocalePickerScreen
import org.zotero.android.screens.settings.debug.SettingsDebugLogScreen
import org.zotero.android.screens.settings.debug.SettingsDebugScreen
import org.zotero.android.screens.settings.quickcopy.SettingsQuickCopyScreen
import org.zotero.android.screens.settings.stylepicker.SettingsStylePickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import org.zotero.android.uicomponents.singlepicker.SinglePickerScreen

internal const val ARG_SETTINGS_CITE_SEARCH = "settingsCiteSearchArgs"

@Composable
internal fun SettingsNavigation(onOpenWebpage: (uri: Uri) -> Unit) {
    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    ZoteroNavHost(
        navController = navController,
        startDestination = SettingsDestinations.SETTINGS,
    ) {
        settingsNavScreens(navigation = navigation, onOpenWebpage = onOpenWebpage)
    }
}

internal fun NavGraphBuilder.settingsNavScreens(
    navigation: ZoteroNavigation,
    onOpenWebpage: (uri: Uri) -> Unit
) {
    settingsScreen(
        onBack = navigation::onBack,
        onOpenWebpage = onOpenWebpage,
        toAccountScreen = navigation::toAccountScreen,
        toDebugScreen = navigation::toDebugScreen,
        toCiteScreen = navigation::toCiteScreen,
        toQuickCopyScreen = navigation::toQuickCopyScreen,
    )
    accountScreen(
        onBack = navigation::onBack,
        onOpenWebpage = onOpenWebpage,
        navigateToSinglePickerScreen = navigation::toSinglePickerScreen
    )
    debugScreen(
        onBack = navigation::onBack,
        toDebugLogScreen = navigation::toDebugLogScreen
    )
    citeScreen(
        navigateToCiteSearch = navigation::toCiteSearchScreen,
        onBack = navigation::onBack,
    )
    citeSearchScreen(
        onBack = navigation::onBack,
    )
    quickCopyScreen(
        navigateToStylePicker = navigation::toStylePicker,
        navigateToCslLocalePicker = navigation::toCslLocalePicker,
        onBack = navigation::onBack,
    )
    stylePickerScreen(
        onBack = navigation::onBack,
    )
    debugLogScreen(onBack = navigation::onBack)
    singlePickerScreen(onBack = navigation::onBack)
    cslLocalePickerScreen(onBack = navigation::onBack)
}

fun NavGraphBuilder.settingsScreen(
    onOpenWebpage: (uri: Uri) -> Unit,
    toAccountScreen: () -> Unit,
    toDebugScreen: () -> Unit,
    toCiteScreen: () -> Unit,
    toQuickCopyScreen: () -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = SettingsDestinations.SETTINGS,
    ) {
        SettingsScreen(
            onBack = onBack,
            toAccountScreen = toAccountScreen,
            onOpenWebpage = onOpenWebpage,
            toDebugScreen = toDebugScreen,
            toCiteScreen = toCiteScreen,
            toQuickCopyScreen = toQuickCopyScreen,
        )
    }
}

private fun NavGraphBuilder.accountScreen(
    onOpenWebpage: (uri: Uri) -> Unit,
    navigateToSinglePickerScreen: () -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = SettingsDestinations.ACCOUNT,
    ) {
        SettingsAccountScreen(
            onBack = onBack,
            onOpenWebpage = onOpenWebpage,
            navigateToSinglePickerScreen = navigateToSinglePickerScreen
        )
    }
}

private fun NavGraphBuilder.debugScreen(
    onBack: () -> Unit,
    toDebugLogScreen: () -> Unit,
) {
    composable(
        route = SettingsDestinations.DEBUG,
    ) {
        SettingsDebugScreen(onBack = onBack, toDebugLogScreen = toDebugLogScreen,)
    }
}

private fun NavGraphBuilder.citeScreen(
    navigateToCiteSearch: (String) -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = SettingsDestinations.CITE,
    ) {
        SettingsCiteScreen(
            onBack = onBack,
            navigateToCiteSearch = navigateToCiteSearch
        )
    }
}

private fun NavGraphBuilder.quickCopyScreen(
    navigateToStylePicker: () -> Unit,
    navigateToCslLocalePicker: () -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = SettingsDestinations.QUICK_COPY,
    ) {
        SettingsQuickCopyScreen(
            onBack = onBack,
            navigateToStylePicker = navigateToStylePicker,
            navigateToCslLocalePicker = navigateToCslLocalePicker
        )
    }
}


private fun NavGraphBuilder.citeSearchScreen(
    onBack: () -> Unit,
) {
    composable(
        route = "${SettingsDestinations.CITE_SEARCH}/{$ARG_SETTINGS_CITE_SEARCH}",
        arguments = listOf(
            navArgument(ARG_SETTINGS_CITE_SEARCH) { type = NavType.StringType },
        ),
    ) {
        SettingsCiteSearchScreen(
            onBack = onBack,
        )
    }
}

private fun NavGraphBuilder.stylePickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = SettingsDestinations.STYLE_PICKER,
    ) {
        SettingsStylePickerScreen(
            onBack = onBack,
        )
    }
}

private fun NavGraphBuilder.debugLogScreen(
    onBack: () -> Unit,
) {
    composable(
        route = SettingsDestinations.DEBUG_LOG,
    ) {
        SettingsDebugLogScreen(onBack = onBack)
    }
}

private fun NavGraphBuilder.singlePickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = SettingsDestinations.SINGLE_PICKER_SCREEN,
        arguments = listOf(),
    ) {
        SinglePickerScreen(onCloseClicked = onBack)
    }
}

private fun NavGraphBuilder.cslLocalePickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = SettingsDestinations.CSL_LOCALE_PICKER,
    ) {
        SettingsCslLocalePickerScreen(
            onBack = onBack,
        )
    }
}

private object SettingsDestinations {
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val DEBUG = "debug"
    const val DEBUG_LOG = "debugLog"
    const val SINGLE_PICKER_SCREEN = "singlePickerScreen"
    const val CSL_LOCALE_PICKER = "cslLocalPicker"
    const val CITE = "cite"
    const val CITE_SEARCH = "citeSearch"
    const val QUICK_COPY = "quickCopy"
    const val STYLE_PICKER = "stylePicker"
}

fun ZoteroNavigation.toSettingsScreen() {
    navController.navigate(SettingsDestinations.SETTINGS)
}

fun ZoteroNavigation.toAccountScreen() {
    navController.navigate(SettingsDestinations.ACCOUNT)
}

fun ZoteroNavigation.toDebugScreen() {
    navController.navigate(SettingsDestinations.DEBUG)
}

fun ZoteroNavigation.toCiteScreen() {
    navController.navigate(SettingsDestinations.CITE)
}

fun ZoteroNavigation.toCiteSearchScreen(args: String) {
    navController.navigate("${SettingsDestinations.CITE_SEARCH}/$args")
}

fun ZoteroNavigation.toStylePicker() {
    navController.navigate(SettingsDestinations.STYLE_PICKER)
}

fun ZoteroNavigation.toQuickCopyScreen() {
    navController.navigate(SettingsDestinations.QUICK_COPY)
}

fun ZoteroNavigation.toDebugLogScreen() {
    navController.navigate(SettingsDestinations.DEBUG_LOG)
}

fun ZoteroNavigation.toSinglePickerScreen() {
    navController.navigate(SettingsDestinations.SINGLE_PICKER_SCREEN)
}

fun ZoteroNavigation.toCslLocalePicker() {
    navController.navigate(SettingsDestinations.CSL_LOCALE_PICKER)
}