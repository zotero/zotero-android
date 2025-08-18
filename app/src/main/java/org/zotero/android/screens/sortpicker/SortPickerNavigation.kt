
package org.zotero.android.screens.sortpicker

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
internal fun SortPickerNavigation() {
    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    ZoteroNavHost(
        navController = navController,
        startDestination = SortPickerDestinations.SORT_PICKER,
    ) {
        sortPickerNavScreens(navigation)
    }
}

internal fun NavGraphBuilder.sortPickerNavScreens(navigation: ZoteroNavigation) {
    sortPickerScreen(
        onBack = navigation::onBack,
        navigateToSinglePickerScreen = navigation::toSinglePickerScreen,
    )
    singlePickerScreen(onBack = navigation::onBack)
}

private fun NavGraphBuilder.sortPickerScreen(
    navigateToSinglePickerScreen: () -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = SortPickerDestinations.SORT_PICKER,
        arguments = listOf(),
    ) {
        SortPickerScreen(
            onBack = onBack,
            navigateToSinglePickerScreen = navigateToSinglePickerScreen,
        )
    }
}

private fun NavGraphBuilder.singlePickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = SortPickerDestinations.SINGLE_PICKER_SCREEN,
        arguments = listOf(),
    ) {
        SinglePickerScreen(onCloseClicked = onBack)
    }
}

private object SortPickerDestinations {
    const val SORT_PICKER = "sortPicker"
    const val SINGLE_PICKER_SCREEN = "singlePickerScreen"
}

fun ZoteroNavigation.toSortPicker() {
    navController.navigate(SortPickerDestinations.SORT_PICKER)
}

private fun ZoteroNavigation.toSinglePickerScreen() {
    navController.navigate(SortPickerDestinations.SINGLE_PICKER_SCREEN)
}