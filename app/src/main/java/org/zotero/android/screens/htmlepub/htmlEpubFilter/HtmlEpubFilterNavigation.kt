package org.zotero.android.screens.htmlepub.htmlEpubFilter

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun HtmlEpubFilterNavigation() {
    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    ZoteroNavHost(
        navController = navController,
        startDestination = HtmlEpubFilterDestinations.HTML_EPUB_FILTER_SCREEN,
    ) {
        htmlEpubFilterNavScreens(navigation = navigation)
    }
}

internal fun NavGraphBuilder.htmlEpubFilterNavScreens(
    navigation: ZoteroNavigation,
) {
    htmlEpubFilterScreen(onBack = navigation::onBack, navigateToTagPicker = navigation::toTagPicker)
    tagPickerScreen(onBack = navigation::onBack)
}

private fun NavGraphBuilder.htmlEpubFilterScreen(
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
) {
    composable(
        route = HtmlEpubFilterDestinations.HTML_EPUB_FILTER_SCREEN,
    ) {
        HtmlEpubFilterScreen(onBack = onBack, navigateToTagPicker = navigateToTagPicker)
    }
}

private fun NavGraphBuilder.tagPickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = HtmlEpubFilterDestinations.TAG_PICKER_SCREEN,
    ) {
        TagPickerScreen(onBack = onBack)
    }
}

private object HtmlEpubFilterDestinations {
    const val HTML_EPUB_FILTER_SCREEN = "htmlEpubFilterScreen"
    const val TAG_PICKER_SCREEN = "tagPickerScreen"
}

fun ZoteroNavigation.toHtmlEpubFilterScreen() {
    navController.navigate(HtmlEpubFilterDestinations.HTML_EPUB_FILTER_SCREEN)
}

private fun ZoteroNavigation.toTagPicker() {
    navController.navigate(HtmlEpubFilterDestinations.TAG_PICKER_SCREEN)
}
