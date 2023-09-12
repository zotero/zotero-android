
package org.zotero.android.pdf

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.dialogFixedDimens
import org.zotero.android.pdf.settings.PdfSettingsScreen
import org.zotero.android.pdffilter.PdfFilterNavigation
import org.zotero.android.pdffilter.pdfFilterNavScreens
import org.zotero.android.pdffilter.toPdfFilterScreen

internal fun NavGraphBuilder.pdfReaderScreenAndNavigation(
    navigation: ZoteroNavigation,
) {
    pdfScreen(
        onBack = navigation::onBack,
        navigateToPdfFilter = navigation::toPdfFilterNavigation,
        navigateToPdfSettings = navigation::toPdfSettings
    )
    dialogFixedDimens(
        modifier = Modifier
            .height(400.dp)
            .width(400.dp),
        route = PdfReaderDestinations.PDF_FILTER_NAVIGATION,
    ) {
        PdfFilterNavigation()
    }
    dialogFixedDimens(
        modifier = Modifier
            .height(500.dp)
            .width(420.dp),
        route = PdfReaderDestinations.PDF_SETTINGS,
    ) {
        PdfSettingsScreen(onBack = navigation::onBack)
    }
}

internal fun NavGraphBuilder.pdfReaderNavScreens(
    navigation: ZoteroNavigation,
) {
    pdfScreen(onBack = navigation::onBack, navigateToPdfFilter = navigation::toPdfFilterScreen, navigateToPdfSettings = navigation::toPdfSettings)
    pdfFilterNavScreens(navigation)
    pdfSettings(navigation)
}

private fun NavGraphBuilder.pdfSettings(navigation: ZoteroNavigation) {
    composable(
        route = PdfReaderDestinations.PDF_SETTINGS,
        arguments = listOf(),
    ) {
        PdfSettingsScreen(onBack = navigation::onBack)
    }
}

private fun NavGraphBuilder.pdfScreen(
    onBack: () -> Unit,
    navigateToPdfFilter: () -> Unit,
    navigateToPdfSettings: () -> Unit,
) {
    composable(
        route = PdfReaderDestinations.PDF_SCREEN,
    ) {
        PdfReaderScreen(
            onBack = onBack,
            navigateToPdfFilter = navigateToPdfFilter,
            navigateToPdfSettings = navigateToPdfSettings
        )
    }
}

private object PdfReaderDestinations {
    const val PDF_FILTER_NAVIGATION = "pdfFilterNavigation"
    const val PDF_SCREEN = "pdfScreen"
    const val PDF_SETTINGS = "pdfSettings"
}

fun ZoteroNavigation.toPdfScreen() {
    navController.navigate(PdfReaderDestinations.PDF_SCREEN)
}

private fun ZoteroNavigation.toPdfFilterNavigation() {
    navController.navigate(PdfReaderDestinations.PDF_FILTER_NAVIGATION)
}

private fun ZoteroNavigation.toPdfSettings() {
    navController.navigate(PdfReaderDestinations.PDF_SETTINGS)
}
