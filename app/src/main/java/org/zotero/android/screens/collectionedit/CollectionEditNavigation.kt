
package org.zotero.android.screens.collectionedit

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.screens.collectionpicker.CollectionPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun CollectionEditNavigation() {
    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    ZoteroNavHost(
        navController = navController,
        startDestination = CollectionEditDestinations.COLLECTION_EDIT,
    ) {
        collectionEditNavScreens(navigation = navigation)
    }
}

internal fun NavGraphBuilder.collectionEditNavScreens(
    navigation: ZoteroNavigation,
) {
    collectionEditScreen(
        onBack = navigation::onBack,
        navigateToCollectionPickerScreen = navigation::toCollectionPickerScreen,
    )
    collectionPickerScreen(onBack = navigation::onBack)
}

private fun NavGraphBuilder.collectionEditScreen(
    navigateToCollectionPickerScreen: () -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = CollectionEditDestinations.COLLECTION_EDIT,
    ) {
        CollectionEditScreen(
            onBack = onBack,
            navigateToCollectionPickerScreen = navigateToCollectionPickerScreen,
        )
    }
}

private fun NavGraphBuilder.collectionPickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = CollectionEditDestinations.COLLECTION_PICKER_SCREEN,
    ) {
        CollectionPickerScreen(onBack = onBack)
    }
}

private object CollectionEditDestinations {
    const val COLLECTION_EDIT = "collectionEdit"
    const val COLLECTION_PICKER_SCREEN = "collectionPickerScreen"
}

fun ZoteroNavigation.toCollectionEditScreen() {
    navController.navigate(CollectionEditDestinations.COLLECTION_EDIT)
}

private fun ZoteroNavigation.toCollectionPickerScreen() {
    navController.navigate(CollectionEditDestinations.COLLECTION_PICKER_SCREEN)
}