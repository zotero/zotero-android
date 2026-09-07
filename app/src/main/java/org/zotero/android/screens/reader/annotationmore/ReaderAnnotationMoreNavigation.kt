package org.zotero.android.screens.reader.annotationmore

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.reader.annotationmore.data.ReaderAnnotationMoreArgs
import org.zotero.android.screens.reader.annotationmore.editpage.ReaderAnnotationEditPageScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun ReaderAnnotationMoreNavigation(args: ReaderAnnotationMoreArgs, onBack: () -> Unit) {
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
        startDestination = ReaderAnnotationMoreDestination.READER_ANNOTATION_MORE_SCREEN,
    ) {
        readerAnnotationMoreNavScreens(args = args, navigation = navigation)
    }
}

internal fun NavGraphBuilder.readerAnnotationMoreNavScreens(
    args: ReaderAnnotationMoreArgs,
    navigation: ZoteroNavigation,
) {
    readerAnnotationMoreScreen(
        args = args,
        onBack = navigation::onBack,
        navigateToPageEdit = navigation::toPageEdit
    )
    pageEditScreen(onBack = navigation::onBack)
}

private fun NavGraphBuilder.readerAnnotationMoreScreen(
    args: ReaderAnnotationMoreArgs,
    onBack: () -> Unit,
    navigateToPageEdit: () -> Unit,
) {
    composable(
        route = ReaderAnnotationMoreDestination.READER_ANNOTATION_MORE_SCREEN,
        arguments = listOf(),
    ) {
        ReaderAnnotationMoreScreen(
            args = args,
            onBack = onBack,
            navigateToPageEdit = navigateToPageEdit
        )
    }
}

private fun NavGraphBuilder.pageEditScreen(
    onBack: () -> Unit,
) {
    composable(
        route = ReaderAnnotationMoreDestination.PAGE_EDIT_SCREEN,
    ) {
        ReaderAnnotationEditPageScreen(onBack = onBack)
    }
}

private object ReaderAnnotationMoreDestination {
    const val READER_ANNOTATION_MORE_SCREEN = "readerAnnotationMoreScreen"
    const val PAGE_EDIT_SCREEN = "pageEditScreen"
}

fun ZoteroNavigation.toReaderAnnotationMoreScreen() {
    navController.navigate(ReaderAnnotationMoreDestination.READER_ANNOTATION_MORE_SCREEN)
}

private fun ZoteroNavigation.toPageEdit() {
    navController.navigate(ReaderAnnotationMoreDestination.PAGE_EDIT_SCREEN)
}
