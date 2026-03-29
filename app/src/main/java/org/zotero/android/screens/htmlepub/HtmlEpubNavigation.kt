
package org.zotero.android.screens.htmlepub

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.dialogFixedDimens
import org.zotero.android.pdf.settings.PdfSettingsScreen
import org.zotero.android.screens.htmlepub.annotation.HtmlEpubAnnotationNavigation
import org.zotero.android.screens.htmlepub.annotation.toHtmlEpubAnnotationScreen
import org.zotero.android.screens.htmlepub.annotationmore.HtmlEpubAnnotationMoreNavigation
import org.zotero.android.screens.htmlepub.annotationmore.toHtmlEpubAnnotationMoreScreen
import org.zotero.android.screens.htmlepub.colorpicker.HtmlEpubReaderColorPickerScreen
import org.zotero.android.screens.htmlepub.htmlEpubFilter.HtmlEpubFilterNavigation
import org.zotero.android.screens.htmlepub.htmlEpubFilter.htmlEpubFilterNavScreens
import org.zotero.android.screens.htmlepub.htmlEpubFilter.toHtmlEpubFilterScreen
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderScreen

internal const val ARG_HTML_EPUB_READER_SCREEN = "htmlEpubReaderArgs"
internal const val ARG_HTML_EPUB_SETTINGS_SCREEN = "htmlEpubSettingsArgs"

internal fun NavGraphBuilder.htmlEpubReaderNavScreensForTablet(
    navigateToTagPicker: () -> Unit,
    onOpenWebpage: (url: String) -> Unit,
    navigation: ZoteroNavigation,
    navController: NavHostController,
) {
    htmlEpubReaderScreen(
        onBack = navigation::onBack,
        navigateToPdfFilter = navigation::toHtmlEpubFilterNavigation,
        navigateToTagPicker = navigateToTagPicker,
        navigateToHtmlEpubAnnotation = navigation::toHtmlEpubAnnotationNavigation,
        navigateToHtmlEpubAnnotationMore = navigation::toHtmlEpubAnnotationMoreNavigation,
        navigateToHtmlEpubColorPicker = navigation::toHtmlEpubColorPicker,
        navigateToHtmlEpubSettings = navigation::toHtmlEpubSettings,
        onOpenWebpage = onOpenWebpage,

    )
    dialogFixedDimens(
        modifier = Modifier
            .height(400.dp)
            .width(400.dp),
        route = HtmlEpubDestinations.HTML_EPUB_FILTER_NAVIGATION,
    ) {
        HtmlEpubFilterNavigation()
    }

    dialogFixedDimens(
        modifier = Modifier
            .height(500.dp)
            .width(420.dp),
        route = "${HtmlEpubDestinations.HTML_EPUB_SETTINGS}/{$ARG_HTML_EPUB_SETTINGS_SCREEN}",
        arguments = listOf(
            navArgument(ARG_HTML_EPUB_SETTINGS_SCREEN) { type = NavType.StringType },
        ),
    ) {
        PdfSettingsScreen(args = null, onBack = navigation::onBack)
    }

    dialogFixedDimens(
        modifier = Modifier
            .height(500.dp)
            .width(420.dp),
        route = HtmlEpubDestinations.HTML_EPUB_ANNOTATION_MORE_NAVIGATION,
    ) {
        HtmlEpubAnnotationMoreNavigation(
            args = ScreenArguments.htmlEpubAnnotationMoreArgs,
            onBack = navigation::onBack
        )
    }
    dialogFixedDimens(
        modifier = Modifier
            .height(220.dp)
            .width(300.dp),
        route = HtmlEpubDestinations.HTML_EPUB_COLOR_PICKER,
    ) {
        HtmlEpubReaderColorPickerScreen(onBack = { navController.popBackStack() })
    }

    dialogFixedDimens(
        modifier = Modifier
            .height(500.dp)
            .width(420.dp),
        route = HtmlEpubDestinations.HTML_EPUB_ANNOTATION_NAVIGATION,
    ) {
        HtmlEpubAnnotationNavigation(
            args = ScreenArguments.htmlEpubAnnotationArgs,
            onBack = navigation::onBack
        )
    }
}

internal fun NavGraphBuilder.htmlEpubReaderNavScreensForPhone(
    navigation: ZoteroNavigation,
    navigateToTagPicker: () -> Unit,
    onOpenWebpage: (url: String) -> Unit,
) {
    htmlEpubReaderScreen(
        onBack = navigation::onBack,
        navigateToPdfFilter = navigation::toHtmlEpubFilterScreen,
        navigateToTagPicker = navigateToTagPicker,
        navigateToHtmlEpubAnnotation = navigation::toHtmlEpubAnnotationScreen,
        navigateToHtmlEpubAnnotationMore = navigation::toHtmlEpubAnnotationMoreScreen,
        navigateToHtmlEpubColorPicker = navigation::toHtmlEpubColorPicker,
        navigateToHtmlEpubSettings = navigation::toHtmlEpubSettings,
        onOpenWebpage = onOpenWebpage,
    )
    htmlEpubFilterNavScreens(navigation)
    htmlEpubColorPicker(navigation)
}
private fun NavGraphBuilder.htmlEpubReaderScreen(
    onBack: () -> Unit,
    navigateToPdfFilter: () -> Unit,
    navigateToTagPicker: () -> Unit,
    navigateToHtmlEpubAnnotation: () -> Unit,
    navigateToHtmlEpubAnnotationMore: () -> Unit,
    navigateToHtmlEpubColorPicker: () -> Unit,
    navigateToHtmlEpubSettings: (args: String) -> Unit,
    onOpenWebpage: (url: String) -> Unit,
) {
    composable(
        route = "${HtmlEpubDestinations.HTML_EPUB_SCREEN}/{$ARG_HTML_EPUB_READER_SCREEN}",
        arguments = listOf(
            navArgument(ARG_HTML_EPUB_READER_SCREEN) { type = NavType.StringType },
        ),
    ) {
        HtmlEpubReaderScreen(
            onBack = onBack,
            navigateToPdfFilter = navigateToPdfFilter,
            navigateToTagPicker = navigateToTagPicker,
            navigateToHtmlEpubAnnotation = navigateToHtmlEpubAnnotation,
            navigateToHtmlEpubAnnotationMore = navigateToHtmlEpubAnnotationMore,
            navigateToHtmlEpubColorPicker = navigateToHtmlEpubColorPicker,
            navigateToHtmlEpubSettings = navigateToHtmlEpubSettings,
            onOpenWebpage = onOpenWebpage
        )
    }
}

private object HtmlEpubDestinations {
    const val HTML_EPUB_SCREEN = "htmlEpubScreen"
    const val HTML_EPUB_FILTER_NAVIGATION = "htmlEpubFilterNavigation"
    const val HTML_EPUB_ANNOTATION_MORE_NAVIGATION = "htmlEpubAnnotationMoreNavigation"
    const val HTML_EPUB_COLOR_PICKER = "htmlEpubColorPicker"
    const val HTML_EPUB_ANNOTATION_NAVIGATION = "htmlEpubAnnotationNavigation"
    const val HTML_EPUB_SETTINGS = "htmlEpubSettings"
}

fun ZoteroNavigation.toHtmlEpubScreen(
    htmlEpubParams: String,
) {
    navController.navigate("${HtmlEpubDestinations.HTML_EPUB_SCREEN}/$htmlEpubParams")
}

private fun ZoteroNavigation.toHtmlEpubAnnotationNavigation() {
    navController.navigate(HtmlEpubDestinations.HTML_EPUB_ANNOTATION_NAVIGATION)
}

private fun ZoteroNavigation.toHtmlEpubAnnotationMoreNavigation() {
    navController.navigate(HtmlEpubDestinations.HTML_EPUB_ANNOTATION_MORE_NAVIGATION)
}

private fun ZoteroNavigation.toHtmlEpubColorPicker() {
    navController.navigate(HtmlEpubDestinations.HTML_EPUB_COLOR_PICKER)
}

private fun NavGraphBuilder.htmlEpubColorPicker(navigation: ZoteroNavigation) {
    composable(
        route = HtmlEpubDestinations.HTML_EPUB_COLOR_PICKER,
        arguments = listOf(),
    ) {
        HtmlEpubReaderColorPickerScreen(onBack = navigation::onBack)
    }
}

private fun ZoteroNavigation.toHtmlEpubFilterNavigation() {
    navController.navigate(HtmlEpubDestinations.HTML_EPUB_FILTER_NAVIGATION)
}

private fun ZoteroNavigation.toHtmlEpubSettings(args: String) {
    navController.navigate("${HtmlEpubDestinations.HTML_EPUB_SETTINGS}/$args")
}
