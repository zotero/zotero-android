package org.zotero.android.screens.reader.annotation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.reader.annotation.data.ReaderAnnotationArgs
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun ReaderAnnotationNavigation(args: ReaderAnnotationArgs, onBack: () -> Unit) {
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
        startDestination = ReaderAnnotationDestination.READER_ANNOTATION_SCREEN,
    ) {
        readerAnnotationNavScreens(args = args, navigation = navigation)
    }
}

internal fun NavGraphBuilder.readerAnnotationNavScreens(
    args: ReaderAnnotationArgs,
    navigation: ZoteroNavigation,
) {

    readerAnnotationScreen(
        args = args,
        onBack = navigation::onBack,
        navigateToTagPicker = navigation::toTagPicker
    )
    tagPickerScreen(onBack = navigation::onBack)
}

private fun NavGraphBuilder.readerAnnotationScreen(
    args: ReaderAnnotationArgs,
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
) {
    composable(
        route = ReaderAnnotationDestination.READER_ANNOTATION_SCREEN,
        arguments = listOf(),
    ) {
        ReaderAnnotationScreen(
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
        route = ReaderAnnotationDestination.TAG_PICKER_SCREEN,
    ) {
        TagPickerScreen(onBack = onBack)
    }
}

private object ReaderAnnotationDestination {
    const val READER_ANNOTATION_SCREEN = "readerAnnotationScreen"
    const val TAG_PICKER_SCREEN = "tagPickerScreen"
}

fun ZoteroNavigation.toReaderAnnotationScreen() {
    navController.navigate(ReaderAnnotationDestination.READER_ANNOTATION_SCREEN)
}

private fun ZoteroNavigation.toTagPicker() {
    navController.navigate(ReaderAnnotationDestination.TAG_PICKER_SCREEN)
}
