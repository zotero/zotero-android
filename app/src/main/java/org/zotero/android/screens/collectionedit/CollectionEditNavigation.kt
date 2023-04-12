
package org.zotero.android.screens.collectionedit

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun CollectionEditNavigation(scaffoldModifier: Modifier = Modifier) {
    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        CreatorEditNavigation(navController, dispatcher)
    }

    ZoteroNavHost(
        navController = navController,
        startDestination = CollectionEditDestinations.COLLECTION_EDIT,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        collectionEditScreen(
            onBack = navigation::onBack,
            navigateToLibraryPickerScreen = navigation::toLibraryPickerScreen,
            scaffoldModifier = scaffoldModifier,
        )
//        singlePickerScreen(onBack = navigation::onBack)
    }
}

private fun NavGraphBuilder.collectionEditScreen(
    scaffoldModifier: Modifier,
    navigateToLibraryPickerScreen: () -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = CollectionEditDestinations.COLLECTION_EDIT,
        arguments = listOf(),
    ) {
        CollectionEditScreen(
            onBack = onBack,
            navigateToLibraryPickerScreen = navigateToLibraryPickerScreen,
            scaffoldModifier = scaffoldModifier,
        )
    }
}

//private fun NavGraphBuilder.singlePickerScreen(
//    onBack: () -> Unit,
//) {
//    composable(
//        route = "${SINGLE_PICKER_SCREEN}",
//        arguments = listOf(),
//    ) {
//        SinglePickerScreen(onCloseClicked = onBack)
//    }
//}

private object CollectionEditDestinations {
    const val COLLECTION_EDIT = "collectionEdit"
    const val LIBRARY_PICKER_SCREEN = "libraryPickerScreen"
}

@SuppressWarnings("UseDataClass")
private class CreatorEditNavigation(
    private val navController: NavHostController,
    private val onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    fun onBack() = onBackPressedDispatcher?.onBackPressed()

    fun toCreatorEdit() {
        navController.navigate(CollectionEditDestinations.COLLECTION_EDIT)
    }

    fun toLibraryPickerScreen() {
        navController.navigate(CollectionEditDestinations.LIBRARY_PICKER_SCREEN)
    }
}
