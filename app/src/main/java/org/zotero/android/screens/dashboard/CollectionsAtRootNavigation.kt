
package org.zotero.android.screens.dashboard

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
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.screenOrDialogFixedHeight
import org.zotero.android.screens.collectionedit.CollectionEditNavigation
import org.zotero.android.screens.collections.CollectionsScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

/**
 * If it's a phone, then this navigation is not shown
 * if it's a tablet, then it only occupies the left portion of the screen.
 */
@Composable
internal fun CollectionsAtRootNavigation(rightPaneNavController: NavHostController) {
    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        CollectionsAtRootNavigation(navController, dispatcher)
    }
    val calculateLayoutType = CustomLayoutSize.calculateLayoutType()
    ZoteroNavHost(
        navController = navController,
        startDestination = CollectionsAtRootDestinations.COLLECTIONS_SCREEN,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        collectionAtRootGraph(
            rightPaneNavController = rightPaneNavController,
            layoutType = calculateLayoutType,
            navController = navController,
            onBack = navigation::onBack,
        )
    }
}

fun NavGraphBuilder.collectionAtRootGraph(
    navController: NavHostController,
    onBack: () -> Unit,
    layoutType: CustomLayoutSize.LayoutType,
    rightPaneNavController: NavHostController? = null,
) {
    collectionsScreen(
        onBack = onBack,
        navigateToAllItems = {
            toAllItems(
                navController = navController,
                rightPaneNavController = rightPaneNavController,
                isTablet = layoutType.isTablet()
            )
        },
        navigateToCollectionEdit = { navController.navigate(CollectionsAtRootDestinations.COLLECTION_EDIT) },
    )
    screenOrDialogFixedHeight(
        route = CollectionsAtRootDestinations.COLLECTION_EDIT,
        layoutType = layoutType,
    ) {
        CollectionEditNavigation()
    }
}

private fun toAllItems(
    navController: NavHostController,
    rightPaneNavController: NavHostController?,
    isTablet: Boolean
) {
    if (isTablet) {
        rightPaneNavController?.navigate(FullScreenDestinations.ALL_ITEMS) {
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
    navigateToCollectionEdit: () -> Unit,
) {
    composable(route = CollectionsAtRootDestinations.COLLECTIONS_SCREEN) {
        CollectionsScreen(
            onBack = onBack,
            navigateToAllItems = navigateToAllItems,
            navigateToCollectionEdit = navigateToCollectionEdit,
        )
    }
}

private object CollectionsAtRootDestinations {
    const val COLLECTIONS_SCREEN = "collectionsScreen"
    const val COLLECTION_EDIT = "collectionEdit"
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

    fun toCollectionEdit() {
        navController.navigate(CollectionsAtRootDestinations.COLLECTION_EDIT)
    }
}