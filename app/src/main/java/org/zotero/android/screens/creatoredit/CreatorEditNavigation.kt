
package org.zotero.android.screens.creatoredit

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
import org.zotero.android.screens.creatoredit.CreatorEditDestinations.SINGLE_PICKER_SCREEN
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import org.zotero.android.uicomponents.singlepicker.SinglePickerScreen

@Composable
internal fun CreatorEditNavigation(scaffoldModifier: Modifier = Modifier) {
    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        CreatorEditNavigation(navController, dispatcher)
    }

    ZoteroNavHost(
        navController = navController,
        startDestination = CreatorEditDestinations.CREATOR_EDIT,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        creatorEditScreen(
            onBack = navigation::onBack,
            navigateToSinglePickerScreen = navigation::toSinglePickerScreen,
            scaffoldModifier = scaffoldModifier,
        )
        singlePickerScreen(onBack = navigation::onBack)
    }
}

private fun NavGraphBuilder.creatorEditScreen(
    scaffoldModifier: Modifier,
    navigateToSinglePickerScreen: () -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = "${CreatorEditDestinations.CREATOR_EDIT}",
        arguments = listOf(),
    ) {
        CreatorEditScreen(
            onBack = onBack,
            navigateToSinglePickerScreen = navigateToSinglePickerScreen,
            scaffoldModifier = scaffoldModifier,
        )
    }
}

private fun NavGraphBuilder.singlePickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = "${SINGLE_PICKER_SCREEN}",
        arguments = listOf(),
    ) {
        SinglePickerScreen(onCloseClicked = onBack)
    }
}

private object CreatorEditDestinations {
    const val CREATOR_EDIT = "creatorEdit"
    const val SINGLE_PICKER_SCREEN = "singlePickerScreen"
}

@SuppressWarnings("UseDataClass")
private class CreatorEditNavigation(
    private val navController: NavHostController,
    private val onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    fun onBack() = onBackPressedDispatcher?.onBackPressed()

    fun toCreatorEdit() {
        navController.navigate("${CreatorEditDestinations.CREATOR_EDIT}")
    }

    fun toSinglePickerScreen() {
        navController.navigate("${CreatorEditDestinations.SINGLE_PICKER_SCREEN}")
    }
}
