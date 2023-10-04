
package org.zotero.android.pdf

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.dialogFixedDimens
import org.zotero.android.pdf.annotation.PdfAnnotationScreen
import org.zotero.android.pdf.reader.PdfReaderScreen
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
        navigateToPdfSettings = navigation::toPdfSettings,
        navigateToPdfAnnotation = navigation::toPdfAnnotation,
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
    dialogFixedDimens(
        modifier = Modifier
            .height(500.dp)
            .width(420.dp),
        route = PdfReaderDestinations.PDF_ANNOTATION_SCREEN,
    ) {
        PdfAnnotationScreen(onBack = navigation::onBack)
    }
}

internal fun NavGraphBuilder.pdfReaderNavScreens(
    navigation: ZoteroNavigation,
) {
    pdfScreen(
        onBack = navigation::onBack,
        navigateToPdfFilter = navigation::toPdfFilterScreen,
        navigateToPdfSettings = navigation::toPdfSettings,
        navigateToPdfAnnotation = navigation::toPdfAnnotation,
    )
    pdfFilterNavScreens(navigation)
    pdfSettings(navigation)
    pdfAnnotationScreen(navigation)
}

private fun NavGraphBuilder.pdfSettings(navigation: ZoteroNavigation) {
    composable(
        route = PdfReaderDestinations.PDF_SETTINGS,
        arguments = listOf(),
    ) {
        PdfSettingsScreen(onBack = navigation::onBack)
    }
}

private fun NavGraphBuilder.pdfAnnotationScreen(navigation: ZoteroNavigation) {
    composable(
        route = PdfReaderDestinations.PDF_ANNOTATION_SCREEN,
        arguments = listOf(),
    ) {
        PdfAnnotationScreen(onBack = navigation::onBack)
    }
}

private fun NavGraphBuilder.pdfScreen(
    onBack: () -> Unit,
    navigateToPdfFilter: () -> Unit,
    navigateToPdfSettings: () -> Unit,
    navigateToPdfAnnotation: () -> Unit,
) {
    composable(
        route = PdfReaderDestinations.PDF_SCREEN,
    ) {
        PdfReaderScreen(
            onBack = onBack,
            navigateToPdfFilter = navigateToPdfFilter,
            navigateToPdfSettings = navigateToPdfSettings,
            navigateToPdfAnnotation = navigateToPdfAnnotation,
        )
    }
}

private object PdfReaderDestinations {
    const val PDF_FILTER_NAVIGATION = "pdfFilterNavigation"
    const val PDF_SCREEN = "pdfScreen"
    const val PDF_SETTINGS = "pdfSettings"
    const val PDF_ANNOTATION_SCREEN = "pdfAnnotationScreen"
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

private fun ZoteroNavigation.toPdfAnnotation() {
    navController.navigate(PdfReaderDestinations.PDF_ANNOTATION_SCREEN)
}
