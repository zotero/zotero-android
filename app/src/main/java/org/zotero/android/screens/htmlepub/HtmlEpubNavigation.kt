
package org.zotero.android.screens.htmlepub

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderScreen

internal const val ARG_HTML_EPUB_READER_SCREEN = "htmlEpubReaderArgs"

internal fun NavGraphBuilder.htmlEpubReaderNavScreensForTablet(
    navigation: ZoteroNavigation,
) {
    htmlEpubReaderScreen(
        onBack = navigation::onBack,
    )
}

internal fun NavGraphBuilder.htmlEpubReaderNavScreensForPhone(
    navigation: ZoteroNavigation,
) {
    htmlEpubReaderScreen(
        onBack = navigation::onBack,
    )
}
private fun NavGraphBuilder.htmlEpubReaderScreen(
    onBack: () -> Unit,
) {
    composable(
        route = "${HtmlEpubDestinations.HTML_EPUB_SCREEN}/{$ARG_HTML_EPUB_READER_SCREEN}",
        arguments = listOf(
            navArgument(ARG_HTML_EPUB_READER_SCREEN) { type = NavType.StringType },
        ),
    ) {
        HtmlEpubReaderScreen(
            onBack = onBack,
        )
    }
}

private object HtmlEpubDestinations {
    const val HTML_EPUB_SCREEN = "htmlEpubScreen"
}

fun ZoteroNavigation.toHtmlEpubScreen(
    htmlEpubParams: String,
) {
    navController.navigate("${HtmlEpubDestinations.HTML_EPUB_SCREEN}/$htmlEpubParams")
}
