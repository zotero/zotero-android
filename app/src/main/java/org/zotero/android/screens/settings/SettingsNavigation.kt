package org.zotero.android.screens.settings

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.screens.settings.account.SettingsAccountScreen
import org.zotero.android.screens.settings.debug.SettingsDebugLogScreen
import org.zotero.android.screens.settings.debug.SettingsDebugScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun SettingsNavigation(onOpenWebpage: (uri: Uri) -> Unit) {
    val navController = rememberAnimatedNavController()
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
    accountScreen(onBack = navigation::onBack, onOpenWebpage = onOpenWebpage)
    debugScreen(onBack = navigation::onBack, toDebugLogScreen = navigation::toDebugLogScreen)
    debugLogScreen(onBack = navigation::onBack)
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
    onBack: () -> Unit,
) {
    composable(
        route = SettingsDestinations.ACCOUNT,
    ) {
        SettingsAccountScreen(onBack = onBack, onOpenWebpage = onOpenWebpage)
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

private object SettingsDestinations {
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val DEBUG = "debug"
    const val DEBUG_LOG = "debugLog"
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