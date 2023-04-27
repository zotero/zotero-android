
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
import org.zotero.android.screens.collectionpicker.CollectionPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun CollectionEditNavigation() {
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
            navigateToCollectionPickerScreen = navigation::toCollectionPickerScreen,
        )
        collectionPickerScreen(onBack = navigation::onBack)
    }
}

private fun NavGraphBuilder.collectionEditScreen(
    navigateToCollectionPickerScreen: () -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = CollectionEditDestinations.COLLECTION_EDIT,
        arguments = listOf(),
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
        arguments = listOf(),
    ) {
        CollectionPickerScreen(onBack = onBack)
    }
}

private object CollectionEditDestinations {
    const val COLLECTION_EDIT = "collectionEdit"
    const val COLLECTION_PICKER_SCREEN = "collectionPickerScreen"
}

@SuppressWarnings("UseDataClass")
private class CreatorEditNavigation(
    private val navController: NavHostController,
    private val onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    fun onBack() = onBackPressedDispatcher?.onBackPressed()

    fun toCollectionPickerScreen() {
        navController.navigate(CollectionEditDestinations.COLLECTION_PICKER_SCREEN)
    }
}
