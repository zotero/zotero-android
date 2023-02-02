@file:OptIn(ExperimentalAnimationApi::class)

package org.zotero.android.dashboard.ui

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
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
            navigateToCreatorEditScreen = navigation::toCreatorEditScreen,
            navigateToCreatorEditDialog = navigation::toCreatorEditDialog,
            navigateToSinglePickerScreen = navigation::toSinglePickerScreen,
            navigateToSinglePickerDialog = navigation::toSinglePickerDialog,
            navigateToAddOrEditNote = navigation::toAddOrEditNote,
            onBack = navigation::onBack
        )
        addNoteScreen(
            onBack = navigation::onBack
        )
        creatorEditScreen()
        creatorEditDialog()
        singlePickerScreen(onBack = navigation::onBack)
        singlePickerDialog(onBack = navigation::onBack)
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
    navigateToCreatorEditScreen: () -> Unit,
    navigateToCreatorEditDialog: () -> Unit,
    navigateToSinglePickerScreen: () -> Unit,
    navigateToSinglePickerDialog: () -> Unit,
    navigateToAddOrEditNote: () -> Unit,
) {
    composable(
        route = "${Destinations.ITEM_DETAILS}",
        arguments = listOf(),
    ) {
        ItemDetailsScreen(
            onBack = onBack,
            navigateToAddOrEditNote = navigateToAddOrEditNote,
            navigateToCreatorEditScreen = navigateToCreatorEditScreen,
            navigateToCreatorEditDialog = navigateToCreatorEditDialog,
            navigateToSinglePickerScreen = navigateToSinglePickerScreen,
            navigateToSinglePickerDialog = navigateToSinglePickerDialog,
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
) {
    composable(
        route = "${Destinations.CREATOR_EDIT_SCREEN}",
        arguments = listOf(),
    ) {
        CreatorEditNavigation()
    }
}

private fun NavGraphBuilder.creatorEditDialog(
) {
    dialog(
        route = "${Destinations.CREATOR_EDIT_DIALOG}",
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
        ) {
            CreatorEditNavigation(scaffoldModifier = Modifier.requiredHeightIn(max = 400.dp))
        }
    }
}

    private fun NavGraphBuilder.singlePickerScreen(
        onBack: () -> Unit,
    ) {
        composable(
            route = "${Destinations.SINGLE_PICKER_SCREEN}",
            arguments = listOf(),
        ) {
            SinglePickerScreen(onCloseClicked = onBack)
        }
    }

    private fun NavGraphBuilder.singlePickerDialog(
        onBack: () -> Unit,
    ) {
        dialog(
            route = "${Destinations.SINGLE_PICKER_DIALOG}",
            dialogProperties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            )
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
            ) {
                SinglePickerScreen(
                    onCloseClicked = onBack,
                    scaffoldModifier = Modifier.fillMaxHeight(0.8f)
                )
            }
        }
}

private object Destinations {
    const val ALL_ITEMS = "allItems"
    const val ITEM_DETAILS = "itemDetails"
    const val ADD_NOTE = "addNote"
    const val CREATOR_EDIT_SCREEN = "creatorEditScreen"
    const val CREATOR_EDIT_DIALOG = "creatorEditDialog"
    const val SINGLE_PICKER_SCREEN = "singlePickerScreen"
    const val SINGLE_PICKER_DIALOG = "singlePickerDialog"
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
    fun toCreatorEditScreen() {
        navController.navigate("${Destinations.CREATOR_EDIT_SCREEN}")
    }
    fun toCreatorEditDialog() {
        navController.navigate("${Destinations.CREATOR_EDIT_DIALOG}")
    }

    fun toSinglePickerScreen() {
        navController.navigate("${Destinations.SINGLE_PICKER_SCREEN}")
    }

    fun toSinglePickerDialog() {
        navController.navigate("${Destinations.SINGLE_PICKER_DIALOG}")
    }
}
