package org.zotero.android.screens.htmlepub.annotationmore

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.htmlepub.annotationmore.data.HtmlEpubAnnotationMoreArgs
import org.zotero.android.screens.htmlepub.annotationmore.editpage.HtmlEpubAnnotationEditPageScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun HtmlEpubAnnotationMoreNavigation(args: HtmlEpubAnnotationMoreArgs, onBack: () -> Unit) {
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
        startDestination = HtmlEpubAnnotationMoreDestination.HTML_EPUB_ANNOTATION_MORE_SCREEN,
    ) {
        htmlEpubAnnotationMoreNavScreens(args = args, navigation = navigation)
    }
}

internal fun NavGraphBuilder.htmlEpubAnnotationMoreNavScreens(
    args: HtmlEpubAnnotationMoreArgs,
    navigation: ZoteroNavigation,
) {
    htmlEpubAnnotationMoreScreen(
        args = args,
        onBack = navigation::onBack,
        navigateToPageEdit = navigation::toPageEdit
    )
    pageEditScreen(onBack = navigation::onBack)
}

private fun NavGraphBuilder.htmlEpubAnnotationMoreScreen(
    args: HtmlEpubAnnotationMoreArgs,
    onBack: () -> Unit,
    navigateToPageEdit: () -> Unit,
) {
    composable(
        route = HtmlEpubAnnotationMoreDestination.HTML_EPUB_ANNOTATION_MORE_SCREEN,
        arguments = listOf(),
    ) {
        HtmlEpubAnnotationMoreScreen(
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
        route = HtmlEpubAnnotationMoreDestination.PAGE_EDIT_SCREEN,
    ) {
        HtmlEpubAnnotationEditPageScreen(onBack = onBack)
    }
}

private object HtmlEpubAnnotationMoreDestination {
    const val HTML_EPUB_ANNOTATION_MORE_SCREEN = "htmlEpubAnnotationMoreScreen"
    const val PAGE_EDIT_SCREEN = "pageEditScreen"
}

fun ZoteroNavigation.toHtmlEpubAnnotationMoreScreen() {
    navController.navigate(HtmlEpubAnnotationMoreDestination.HTML_EPUB_ANNOTATION_MORE_SCREEN)
}

private fun ZoteroNavigation.toPageEdit() {
    navController.navigate(HtmlEpubAnnotationMoreDestination.PAGE_EDIT_SCREEN)
}
