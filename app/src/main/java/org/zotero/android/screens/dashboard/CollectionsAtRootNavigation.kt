
package org.zotero.android.screens.dashboard

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
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
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.collectionedit.CollectionEditNavigation
import org.zotero.android.screens.collections.CollectionsScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

/**
 * If it's a phone, then this navigation is not shown
 * if it's a tablet, then it only occupies the left portion of the screen.
 */
@Composable
internal fun CollectionsAtRootNavigation(
) {
    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        CollectionsAtRootNavigation(navController, dispatcher)
    }
    val isTablet = CustomLayoutSize.calculateLayoutType().isTablet()
    ZoteroNavHost(
        navController = navController,
        startDestination = CollectionsAtRootDestinations.COLLECTIONS_SCREEN,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        collectionAsRootGraph(
            isTablet = isTablet,
            navController = navController,
            onBack = navigation::onBack,
        )
    }
}

fun NavGraphBuilder.collectionAsRootGraph(
    navController: NavHostController,
    onBack: () -> Unit,
    isTablet: Boolean,
) {
    collectionsScreen(
        onBack = onBack,
        navigateToAllItems = { toAllItems(navController, isTablet) },
        navigateToCollectionEditScreen = { navController.navigate(CollectionsAtRootDestinations.COLLECTION_EDIT_SCREEN) },
        navigateToCollectionEditDialog = { navController.navigate(CollectionsAtRootDestinations.COLLECTION_EDIT_DIALOG) }
    )
    collectionEditScreen()
    collectionEditDialog()
}

private fun toAllItems(navController: NavHostController,isTablet: Boolean) {
    if (isTablet) {
        navController.navigate(FullScreenDestinations.ALL_ITEMS) {
            popUpTo(0)
        }
    } else {
        navController.popBackStack(navController.graph.id, inclusive = true)
        navController.navigate(FullScreenDestinations.COLLECTIONS_SCREEN)
        navController.navigate(FullScreenDestinations.ALL_ITEMS)
    }
}

private fun NavGraphBuilder.collectionsScreen(
    onBack: () -> Unit,
    navigateToAllItems: () -> Unit,
    navigateToCollectionEditScreen: () -> Unit,
    navigateToCollectionEditDialog: () -> Unit,
) {
    composable(route = CollectionsAtRootDestinations.COLLECTIONS_SCREEN) {
        CollectionsScreen(
            onBack = onBack,
            navigateToAllItems = navigateToAllItems,
            navigateToCollectionEditScreen = navigateToCollectionEditScreen,
            navigateToCollectionEditDialog = navigateToCollectionEditDialog,
        )
    }
}

private fun NavGraphBuilder.collectionEditScreen(
) {
    composable(
        route = CollectionsAtRootDestinations.COLLECTION_EDIT_SCREEN,
        arguments = listOf(),
    ) {
        CollectionEditNavigation()
    }
}

private fun NavGraphBuilder.collectionEditDialog(
) {
    dialog(
        route = CollectionsAtRootDestinations.COLLECTION_EDIT_DIALOG,
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
        ) {
            CollectionEditNavigation(scaffoldModifier = Modifier.requiredHeightIn(max = 400.dp))
        }
    }
}

private object CollectionsAtRootDestinations {
    const val COLLECTIONS_SCREEN = "collectionsScreen"
    const val COLLECTION_EDIT_SCREEN = "collectionEditScreen"
    const val COLLECTION_EDIT_DIALOG = "collectionDialog"
}

@SuppressWarnings("UseDataClass")
private class CollectionsAtRootNavigation(
    private val navController: NavHostController,
    private val onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    fun onBack() = onBackPressedDispatcher?.onBackPressed()

    fun toCollectionsScreen() {
        navController.navigate(CollectionsAtRootDestinations.COLLECTIONS_SCREEN)
    }

    fun toCollectionEditScreen() {
        navController.navigate(CollectionsAtRootDestinations.COLLECTION_EDIT_SCREEN)
    }

    fun toCollectionEditDialog() {
        navController.navigate(CollectionsAtRootDestinations.COLLECTION_EDIT_DIALOG)
    }
}