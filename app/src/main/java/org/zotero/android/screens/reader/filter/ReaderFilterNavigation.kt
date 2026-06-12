package org.zotero.android.screens.reader.filter

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.reader.filter.data.ReaderFilterArgs
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun ReaderFilterNavigation(
    args: ReaderFilterArgs,
    onBack: () -> Unit
) {
    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val isTablet = layoutType.isTablet()
    if (!isTablet) {
        BackHandler(onBack = {
            onBack()
        })
    }
    ZoteroNavHost(
        navController = navController,
        startDestination = ReaderFilterDestinations.READER_FILTER_SCREEN,
    ) {
        readerFilterNavScreens(args = args, navigation = navigation)
    }
}

internal fun NavGraphBuilder.readerFilterNavScreens(
    navigation: ZoteroNavigation,
    args: ReaderFilterArgs,
) {
    readerFilterScreen(
        args = args,
        onBack = navigation::onBack,
        navigateToTagPicker = navigation::toTagPicker
    )
    tagPickerScreen(onBack = navigation::onBack)
}

private fun NavGraphBuilder.readerFilterScreen(
    args: ReaderFilterArgs,
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
) {
    composable(
        route = ReaderFilterDestinations.READER_FILTER_SCREEN,
    ) {
        ReaderFilterScreen(
            args = args,
            onBack = onBack,
            navigateToTagPicker = navigateToTagPicker
        )
    }
}

private fun NavGraphBuilder.tagPickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = ReaderFilterDestinations.TAG_PICKER_SCREEN,
    ) {
        TagPickerScreen(onBack = onBack)
    }
}

private object ReaderFilterDestinations {
    const val READER_FILTER_SCREEN = "readerFilterScreen"
    const val TAG_PICKER_SCREEN = "tagPickerScreen"
}

fun ZoteroNavigation.toReaderFilterScreen() {
    navController.navigate(ReaderFilterDestinations.READER_FILTER_SCREEN)
}

private fun ZoteroNavigation.toTagPicker() {
    navController.navigate(ReaderFilterDestinations.TAG_PICKER_SCREEN)
}
