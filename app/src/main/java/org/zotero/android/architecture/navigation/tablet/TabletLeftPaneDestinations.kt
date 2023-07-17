
package org.zotero.android.architecture.navigation.tablet

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.architecture.navigation.CommonScreenDestinations
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.collectionsScreen
import org.zotero.android.architecture.navigation.dialogFixedMaxHeight
import org.zotero.android.architecture.navigation.librariesScreen
import org.zotero.android.screens.collectionedit.CollectionEditNavigation
import org.zotero.android.screens.settings.SettingsNavigation
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun TabletLeftPaneNavigation(
    onOpenWebpage: (uri: Uri) -> Unit,
    navigateAndPopAllItemsScreen: () -> Unit,
) {
    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    ZoteroNavHost(
        navController = navController,
        startDestination = CommonScreenDestinations.COLLECTIONS_SCREEN,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        collectionsScreen(
            onBack = navigation::onBack,
            navigateToAllItems = navigateAndPopAllItemsScreen,
            navigateToLibraries = {
                navController.navigate(CommonScreenDestinations.LIBRARIES_SCREEN)
            },
            navigateToCollectionEdit = navigation::toCollectionEditNavigation,
        )

        librariesScreen(
            navigateToCollectionsScreen = {
                navController.popBackStack(navController.graph.id, inclusive = true)
                navController.navigate(CommonScreenDestinations.LIBRARIES_SCREEN)
                navController.navigate(CommonScreenDestinations.COLLECTIONS_SCREEN)
            },
            onSettingsTapped = navigation::toSettingsNavigation
        )
        dialogFixedMaxHeight(
            route = TabletLeftPaneDestinations.COLLECTION_EDIT_NAVIGATION,
        ) {
            CollectionEditNavigation()
        }

        dialogFixedMaxHeight(
            route = TabletLeftPaneDestinations.SETTINGS_NAVIGATION,
        ) {
            SettingsNavigation(onOpenWebpage = onOpenWebpage)
        }
    }
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