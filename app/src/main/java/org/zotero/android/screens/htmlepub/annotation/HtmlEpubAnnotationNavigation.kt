package org.zotero.android.screens.htmlepub.annotation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.htmlepub.annotation.data.HtmlEpubAnnotationArgs
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun HtmlEpubAnnotationNavigation(args: HtmlEpubAnnotationArgs, onBack: () -> Unit) {
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
        startDestination = HtmlEpubAnnotationDestinatiosn.HTML_EPUB_ANNOTATION_SCREEN,
    ) {
        htmlEpubAnnotationNavScreens(args = args, navigation = navigation)
    }
}

internal fun NavGraphBuilder.htmlEpubAnnotationNavScreens(
    args: HtmlEpubAnnotationArgs,
    navigation: ZoteroNavigation,
) {

    htmlEpubAnnotationScreen(
        args = args,
        onBack = navigation::onBack,
        navigateToTagPicker = navigation::toTagPicker
    )
    tagPickerScreen(onBack = navigation::onBack)
}

private fun NavGraphBuilder.htmlEpubAnnotationScreen(
    args: HtmlEpubAnnotationArgs,
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
) {
    composable(
        route = HtmlEpubAnnotationDestinatiosn.HTML_EPUB_ANNOTATION_SCREEN,
        arguments = listOf(),
    ) {
        HtmlEpubAnnotationScreen(
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
        route = HtmlEpubAnnotationDestinatiosn.TAG_PICKER_SCREEN,
    ) {
        TagPickerScreen(onBack = onBack)
    }
}

private object HtmlEpubAnnotationDestinatiosn {
    const val HTML_EPUB_ANNOTATION_SCREEN = "htmlEpubAnnotationScreen"
    const val TAG_PICKER_SCREEN = "tagPickerScreen"
}

fun ZoteroNavigation.toHtmlEpubAnnotationScreen() {
    navController.navigate(HtmlEpubAnnotationDestinatiosn.HTML_EPUB_ANNOTATION_SCREEN)
}

private fun ZoteroNavigation.toTagPicker() {
    navController.navigate(HtmlEpubAnnotationDestinatiosn.TAG_PICKER_SCREEN)
}
