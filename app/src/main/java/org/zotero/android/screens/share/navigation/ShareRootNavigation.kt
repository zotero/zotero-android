package org.zotero.android.screens.share.navigation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.share.ShareScreen
import org.zotero.android.screens.share.sharecollectionpicker.ShareCollectionPickerScreen
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun ShareRootNavigation(
) {
    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    val layoutType = CustomLayoutSize.calculateLayoutType()
    if (layoutType.isTablet()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
//                    .fillMaxWidth(0.7f)
//                    .fillMaxHeight(0.6f)
//                    .align(Alignment.Center)
            ) {
                ShareRootNavHost(navController, navigation)
            }
        }
    } else {
        Column {
            ShareRootNavHost(navController, navigation)
        }
    }

}

@Composable
private fun ShareRootNavHost(
    navController: NavHostController,
    navigation: ZoteroNavigation
) {
    ZoteroNavHost(
        navController = navController,
        startDestination = ShareRootDestinations.SHARE_SCREEN,
    ) {
        shareScreen(
            onBack = navigation::onBack,
            navigateToTagPicker = navigation::toTagPicker,
            navigateToCollectionPicker = navigation::toCollectionPicker
        )
        tagPickerScreen(onBack = navigation::onBack)
        shareCollectionPickerScreen(onBack = navigation::onBack)
    }
}

private fun NavGraphBuilder.shareScreen(
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
    navigateToCollectionPicker: () -> Unit,
) {
    composable(
        route = ShareRootDestinations.SHARE_SCREEN,
        arguments = listOf(),
    ) {
        ShareScreen(
            onBack = onBack,
            navigateToTagPicker = navigateToTagPicker,
            navigateToCollectionPicker = navigateToCollectionPicker
        )
    }
}

private fun NavGraphBuilder.shareCollectionPickerScreen(onBack: () -> Unit) {
    composable(
        route = ShareRootDestinations.SHARE_COLLECTION_PICKER_SCREEN,
        arguments = listOf(),
    ) {
        ShareCollectionPickerScreen(onBack = onBack)
    }
}

private fun NavGraphBuilder.tagPickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = ShareRootDestinations.TAG_PICKER_SCREEN,
    ) {
        TagPickerScreen(onBack = onBack)
    }
}

private fun ZoteroNavigation.toTagPicker() {
    navController.navigate(ShareRootDestinations.TAG_PICKER_SCREEN)
}

private fun ZoteroNavigation.toCollectionPicker() {
    navController.navigate(ShareRootDestinations.SHARE_COLLECTION_PICKER_SCREEN)
}

private object ShareRootDestinations {
    const val SHARE_SCREEN = "shareScreen"
    const val SHARE_COLLECTION_PICKER_SCREEN = "shareCollectionPickerScreen"
    const val TAG_PICKER_SCREEN = "shareTagPickerScreen"
}

