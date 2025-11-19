
package org.zotero.android.architecture.navigation.tablet

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.Consumable
import org.zotero.android.architecture.navigation.ARG_COLLECTIONS_SCREEN
import org.zotero.android.architecture.navigation.CommonScreenDestinations
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.collectionsScreen
import org.zotero.android.architecture.navigation.dialogDynamicHeight
import org.zotero.android.architecture.navigation.dialogFixedMaxHeight
import org.zotero.android.architecture.navigation.librariesScreen
import org.zotero.android.screens.collectionedit.CollectionEditNavigation
import org.zotero.android.screens.dashboard.DashboardViewEffect
import org.zotero.android.screens.settings.SettingsNavigation
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun TabletLeftPaneNavigation(
    viewEffect: Consumable<DashboardViewEffect>?,
    onOpenWebpage: (url: String) -> Unit,
    navigateAndPopAllItemsScreen: (String) -> Unit,
    onExitApp:() -> Unit,
    collectionDefaultValue: String
) {
    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    LaunchedEffect(key1 = viewEffect) {
        val consumedEffect = viewEffect?.consume()
        when (consumedEffect) {
            null -> {
                Unit
            }

            is DashboardViewEffect.NavigateToCollectionsScreen -> {
                navigateToCollectionsScreen(
                    navController = navController,
                    collectionArgs = consumedEffect.screenArgs
                )
            }
        }
    }
    ZoteroNavHost(
        navController = navController,
        startDestination = "${CommonScreenDestinations.COLLECTIONS_SCREEN}/{$ARG_COLLECTIONS_SCREEN}",
    ) {
        collectionsScreen(
            collectionDefaultValue = collectionDefaultValue,
            onBack = navigation::onBack,
            navigateToAllItems = navigateAndPopAllItemsScreen,
            navigateToLibraries = {
                navController.navigate(CommonScreenDestinations.LIBRARIES_SCREEN)
            },
            navigateToCollectionEdit = navigation::toCollectionEditNavigation,
            isTablet = true
        )

        librariesScreen(
            navigateToCollectionsScreen = {
                navigateToCollectionsScreen(navController, it)
            },
            onSettingsTapped = navigation::toSettingsNavigation,
            onExitApp = onExitApp,
        )
        dialogFixedMaxHeight(
            route = TabletLeftPaneDestinations.COLLECTION_EDIT_NAVIGATION,
        ) {
            CollectionEditNavigation()
        }

        dialogDynamicHeight(
            route = TabletLeftPaneDestinations.SETTINGS_NAVIGATION,
        ) {
            SettingsNavigation(onOpenWebpage = onOpenWebpage)
        }
    }
}

private fun navigateToCollectionsScreen(navController: NavHostController, collectionArgs: String) {
//    navController.popBackStack(navController.graph.id, inclusive = true)
//    navController.navigate(CommonScreenDestinations.LIBRARIES_SCREEN)
    navController.navigate("${CommonScreenDestinations.COLLECTIONS_SCREEN}/$collectionArgs")
}

private object TabletLeftPaneDestinations {
    const val COLLECTION_EDIT_NAVIGATION = "collectionEditNavigation"
    const val SETTINGS_NAVIGATION = "settingsNavigation"
}

private fun ZoteroNavigation.toSettingsNavigation() {
    navController.navigate(TabletLeftPaneDestinations.SETTINGS_NAVIGATION)
}

private fun ZoteroNavigation.toCollectionEditNavigation() {
    navController.navigate(TabletLeftPaneDestinations.COLLECTION_EDIT_NAVIGATION)
}