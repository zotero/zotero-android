
package org.zotero.android.pdf

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.dialogFixedDimens
import org.zotero.android.pdffilter.PdfFilterNavigation
import org.zotero.android.pdffilter.pdfFilterNavScreens
import org.zotero.android.pdffilter.toPdfFilterScreen

internal fun NavGraphBuilder.pdfReaderScreenAndNavigation(navigation: ZoteroNavigation) {
    pdfScreen(onBack = navigation::onBack, navigateToPdfFilter = navigation::toPdfFilterNavigation)
    dialogFixedDimens(
        modifier = Modifier
            .height(400.dp)
            .width(400.dp),
        route = PdfReaderDestinations.PDF_FILTER_NAVIGATION,
    ) {
        PdfFilterNavigation()
    }
}

internal fun NavGraphBuilder.pdfReaderNavScreens(
    navigation: ZoteroNavigation,
) {
    pdfScreen(onBack = navigation::onBack, navigateToPdfFilter = navigation::toPdfFilterScreen)
    pdfFilterNavScreens(navigation)
}

private fun NavGraphBuilder.pdfScreen(
    onBack: () -> Unit,
    navigateToPdfFilter: () -> Unit,
) {
    composable(
        route = PdfReaderDestinations.PDF_SCREEN,
    ) {
        PdfReaderScreen(onBack = onBack, navigateToPdfFilter = navigateToPdfFilter)
    }
}

private object PdfReaderDestinations {
    const val PDF_FILTER_NAVIGATION = "pdfFilterNavigation"
    const val PDF_SCREEN = "pdfScreen"
}

fun ZoteroNavigation.toPdfScreen() {
    navController.navigate(PdfReaderDestinations.PDF_SCREEN)
}

private fun ZoteroNavigation.toPdfFilterNavigation() {
    navController.navigate(PdfReaderDestinations.PDF_FILTER_NAVIGATION)
}
