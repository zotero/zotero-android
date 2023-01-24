@file:OptIn(ExperimentalAnimationApi::class)

package org.zotero.android.dashboard.ui

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.itemdetails.ItemDetailsScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@ExperimentalAnimationApi
@Composable
internal fun DashboardNavigation(onPickFile: () -> Unit) {
    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        Navigation(navController, dispatcher)
    }

    ZoteroNavHost(
        navController = navController,
        startDestination = Destinations.ALL_ITEMS,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        allItemsScreen(
            onBack = navigation::onBack,
            onPickFile = onPickFile,
            navigateToItemDetails = navigation::toItemDetails,
            navigateToAddOrEditNote = navigation::toAddOrEditNote
        )
        itemDetailsScreen(
            navigateToCreatorEdit = navigation::toCreatorEdit,
            navigateToAddOrEditNote = navigation::toAddOrEditNote,
            onBack = navigation::onBack
        )
        addNoteScreen(
            onBack = navigation::onBack
        )
        creatorEditScreen(
            onBack = navigation::onBack,
        )
    }
}

private fun NavGraphBuilder.allItemsScreen(
    onBack: () -> Unit,
    navigateToItemDetails: () -> Unit,
    onPickFile: () -> Unit,
    navigateToAddOrEditNote: () -> Unit,
) {
    composable(route = Destinations.ALL_ITEMS) {
        AllItemsScreen(
            onBack = onBack,
            onPickFile = onPickFile,
            navigateToAddOrEditNote = navigateToAddOrEditNote,
            navigateToItemDetails = navigateToItemDetails,
        )
    }
}

private fun NavGraphBuilder.itemDetailsScreen(
    onBack: () -> Unit,
    navigateToCreatorEdit: () -> Unit,
    navigateToAddOrEditNote: () -> Unit,
) {
    composable(
        route = "${Destinations.ITEM_DETAILS}",
        arguments = listOf(),
    ) {
        ItemDetailsScreen(
            onBack = onBack,
            navigateToAddOrEditNote = navigateToAddOrEditNote,
            navigateToCreatorEdit = navigateToCreatorEdit
        )
    }
}

private fun NavGraphBuilder.addNoteScreen(
    onBack: () -> Unit,
) {
    composable(
        route = "${Destinations.ADD_NOTE}",
        arguments = listOf(),
    ) {
        AddNoteScreen(
            onBack = onBack,
        )
    }
}

private fun NavGraphBuilder.creatorEditScreen(
    onBack: () -> Unit,
) {
    composable(
        route = "${Destinations.CREATOR_EDIT}",
        arguments = listOf(),
    ) {
        CreatorEditScreen(
            onBack = onBack,
        )
    }
}

private object Destinations {
    const val ALL_ITEMS = "allItems"
    const val ITEM_DETAILS = "itemDetails"
    const val ADD_NOTE = "addNote"
    const val CREATOR_EDIT = "creatorEdit"
}

@SuppressWarnings("UseDataClass")
private class Navigation(
    private val navController: NavHostController,
    private val onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    fun onBack() = onBackPressedDispatcher?.onBackPressed()

    fun toItemDetails() {
        navController.navigate("${Destinations.ITEM_DETAILS}")
    }

    fun toAddOrEditNote() {
        navController.navigate("${Destinations.ADD_NOTE}")
    }
    fun toCreatorEdit() {
        navController.navigate("${Destinations.CREATOR_EDIT}")
    }
}
