
package org.zotero.android.screens.creatoredit

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import org.zotero.android.uicomponents.singlepicker.SinglePickerScreen

@Composable
internal fun CreatorEditNavigation() {
    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }

    ZoteroNavHost(
        navController = navController,
        startDestination = CreatorEditDestinations.CREATOR_EDIT,
    ) {
        creatorEditNavScreens(navigation)
    }
}

internal fun NavGraphBuilder.creatorEditNavScreens(
    navigation: ZoteroNavigation,
) {
    creatorEditScreen(
        onBack = navigation::onBack,
        navigateToSinglePickerScreen = navigation::toSinglePickerScreen,
    )
    singlePickerScreen(onBack = navigation::onBack)
}

private fun NavGraphBuilder.creatorEditScreen(
    navigateToSinglePickerScreen: () -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = CreatorEditDestinations.CREATOR_EDIT,
    ) {
        CreatorEditScreen(
            onBack = onBack,
            navigateToSinglePickerScreen = navigateToSinglePickerScreen,
        )
    }
}

private fun NavGraphBuilder.singlePickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = CreatorEditDestinations.SINGLE_PICKER_SCREEN,
    ) {
        SinglePickerScreen(onCloseClicked = onBack)
    }
}

private object CreatorEditDestinations {
    const val CREATOR_EDIT = "creatorEdit"
    const val SINGLE_PICKER_SCREEN = "singlePickerScreen"
}

fun ZoteroNavigation.toCreatorEdit() {
    navController.navigate(CreatorEditDestinations.CREATOR_EDIT)
}

private fun ZoteroNavigation.toSinglePickerScreen() {
    navController.navigate(CreatorEditDestinations.SINGLE_PICKER_SCREEN)
}
