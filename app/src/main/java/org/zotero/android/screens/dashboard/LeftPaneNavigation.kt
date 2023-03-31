
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
import org.zotero.android.screens.collections.CollectionsScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

/**
 * If it's a phone, then this navigation is not shown
 * if it's a tablet, then it only occupies the left portion of the screen.
 */
@Composable
internal fun LeftPaneNavigation(
    navigateToAllItems: () -> Unit,
) {
    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        LeftPaneNavigation(navController, dispatcher)
    }

    ZoteroNavHost(
        navController = navController,
        startDestination = LeftPaneDestinations.COLLECTIONS_SCREEN,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        collectionsScreenLeftPane(
            onBack = navigation::onBack,
            navigateToAllItems = navigateToAllItems
        )

    }
}

private fun NavGraphBuilder.collectionsScreenLeftPane(
    onBack: () -> Unit,
    navigateToAllItems: () -> Unit,
) {
    composable(route = LeftPaneDestinations.COLLECTIONS_SCREEN) {
        CollectionsScreen(
            onBack = onBack,
            navigateToAllItems = navigateToAllItems,
        )
    }
}

private object LeftPaneDestinations {
    const val COLLECTIONS_SCREEN = "collectionsScreen"
}

@SuppressWarnings("UseDataClass")
private class LeftPaneNavigation(
    private val navController: NavHostController,
    private val onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    fun onBack() = onBackPressedDispatcher?.onBackPressed()

    fun toCollectionsScreen() {
        navController.navigate(LeftPaneDestinations.COLLECTIONS_SCREEN)
    }
}
