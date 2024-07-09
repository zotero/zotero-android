package org.zotero.android.screens.settings

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.screens.settings.account.SettingsAccountScreen
import org.zotero.android.screens.settings.debug.SettingsDebugLogScreen
import org.zotero.android.screens.settings.debug.SettingsDebugScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import org.zotero.android.uicomponents.singlepicker.SinglePickerScreen

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
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
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
    debugLogScreen(onBack = navigation::onBack)
    singlePickerScreen(onBack = navigation::onBack)
}

fun NavGraphBuilder.settingsScreen(
    onOpenWebpage: (uri: Uri) -> Unit,
    toAccountScreen: () -> Unit,
    toDebugScreen: () -> Unit,
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

private object SettingsDestinations {
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val DEBUG = "debug"
    const val DEBUG_LOG = "debugLog"
    const val SINGLE_PICKER_SCREEN = "singlePickerScreen"
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

fun ZoteroNavigation.toDebugLogScreen() {
    navController.navigate(SettingsDestinations.DEBUG_LOG)
}

fun ZoteroNavigation.toSinglePickerScreen() {
    navController.navigate(SettingsDestinations.SINGLE_PICKER_SCREEN)
}