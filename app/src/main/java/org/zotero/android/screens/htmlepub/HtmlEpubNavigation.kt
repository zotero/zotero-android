
package org.zotero.android.screens.htmlepub

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.dialogFixedDimens
import org.zotero.android.screens.htmlepub.htmlEpubFilter.HtmlEpubFilterNavigation
import org.zotero.android.screens.htmlepub.htmlEpubFilter.htmlEpubFilterNavScreens
import org.zotero.android.screens.htmlepub.htmlEpubFilter.toHtmlEpubFilterScreen
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderScreen

internal const val ARG_HTML_EPUB_READER_SCREEN = "htmlEpubReaderArgs"

internal fun NavGraphBuilder.htmlEpubReaderNavScreensForTablet(
    navigation: ZoteroNavigation,
) {
    htmlEpubReaderScreen(
        onBack = navigation::onBack,
        navigateToPdfFilter = navigation::toHtmlEpubFilterScreen,
    )
    dialogFixedDimens(
        modifier = Modifier
            .height(400.dp)
            .width(400.dp),
        route = HtmlEpubDestinations.HTML_EPUB_FILTER_NAVIGATION,
    ) {
        HtmlEpubFilterNavigation()
    }
}

internal fun NavGraphBuilder.htmlEpubReaderNavScreensForPhone(
    navigation: ZoteroNavigation,
) {
    htmlEpubReaderScreen(
        onBack = navigation::onBack,
        navigateToPdfFilter = navigation::toHtmlEpubFilterScreen,
    )
    htmlEpubFilterNavScreens(navigation)
}
private fun NavGraphBuilder.htmlEpubReaderScreen(
    onBack: () -> Unit,
    navigateToPdfFilter: () -> Unit,
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
        )
    }
}

private object HtmlEpubDestinations {
    const val HTML_EPUB_SCREEN = "htmlEpubScreen"
    const val HTML_EPUB_FILTER_NAVIGATION = "htmlEpubFilterNavigation"
}

fun ZoteroNavigation.toHtmlEpubScreen(
    htmlEpubParams: String,
) {
    navController.navigate("${HtmlEpubDestinations.HTML_EPUB_SCREEN}/$htmlEpubParams")
}
