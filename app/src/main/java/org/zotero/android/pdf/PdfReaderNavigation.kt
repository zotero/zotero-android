
package org.zotero.android.pdf

import android.content.Context
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.zotero.android.androidx.content.longToast
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.dialogDynamicHeight
import org.zotero.android.architecture.navigation.dialogFixedDimens
import org.zotero.android.pdf.annotation.PdfAnnotationNavigation
import org.zotero.android.pdf.annotation.toPdfAnnotationScreen
import org.zotero.android.pdf.annotationmore.PdfAnnotationMoreNavigation
import org.zotero.android.pdf.annotationmore.toPdfAnnotationMoreScreen
import org.zotero.android.pdf.colorpicker.PdfReaderColorPickerScreen
import org.zotero.android.pdf.pdffilter.PdfFilterNavigation
import org.zotero.android.pdf.pdffilter.pdfFilterNavScreens
import org.zotero.android.pdf.pdffilter.toPdfFilterScreen
import org.zotero.android.pdf.reader.PdfReaderScreen
import org.zotero.android.pdf.reader.plainreader.PdfPlanReaderScreen
import org.zotero.android.pdf.settings.PdfSettingsScreen
import org.zotero.android.screens.citation.singlecitation.SingleCitationScreen
import java.io.File

internal const val ARG_PDF_SCREEN = "pdfScreenArgs"
internal const val ARG_PDF_SETTINGS_SCREEN = "pdfSettingsScreen"
internal const val ARG_PDF_PLAIN_READER_SCREEN = "pdfPlainReaderScreen"

internal fun NavGraphBuilder.pdfReaderScreenAndNavigationForTablet(
    onExportPdf: (file: File) -> Unit,
    navigation: ZoteroNavigation,
    navigateToTagPickerDialog: () -> Unit,
) {
    pdfScreen(
        onBack = navigation::onBack,
        onExportPdf = onExportPdf,
        navigateToPdfFilter = navigation::toPdfFilterNavigation,
        navigateToPdfSettings = navigation::toPdfSettings,
        navigateToPdfPlainReader = navigation::toPdfPlainReader,
        navigateToPdfColorPicker = navigation::toPdfColorPicker,
        navigateToPdfAnnotation = navigation::toPdfAnnotationNavigation,
        navigateToPdfAnnotationMore = navigation::toPdfAnnotationMoreNavigation,
        navigateToTagPicker = navigateToTagPickerDialog,
        navigateToSingleCitationScreen = navigation::toSingleCitationScreen,
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
        route = "${PdfReaderDestinations.PDF_SETTINGS}/{$ARG_PDF_SETTINGS_SCREEN}",
        arguments = listOf(
            navArgument(ARG_PDF_SETTINGS_SCREEN) { type = NavType.StringType },
        ),
    ) {
        PdfSettingsScreen(args = null, onBack = navigation::onBack)
    }
    dialogDynamicHeight(
        route = PdfReaderDestinations.SINGLE_CITATION_PICKER_DIALOG,
    ) {
        SingleCitationScreen(onBack = navigation::onBack)
    }
    dialogFixedDimens(
        modifier = Modifier
            .height(500.dp)
            .width(420.dp),
        route = PdfReaderDestinations.PDF_ANNOTATION_NAVIGATION,
    ) {
        PdfAnnotationNavigation(
            args = ScreenArguments.pdfAnnotationArgs,
            onBack = navigation::onBack
        )
    }
    dialogFixedDimens(
        modifier = Modifier
            .height(500.dp)
            .width(420.dp),
        route = PdfReaderDestinations.PDF_ANNOTATION_MORE_NAVIGATION,
    ) {
        PdfAnnotationMoreNavigation(
            args = ScreenArguments.pdfAnnotationMoreArgs,
            onBack = navigation::onBack
        )
    }
    dialogFixedDimens(
        modifier = Modifier
            .height(220.dp)
            .width(300.dp),
        route = PdfReaderDestinations.PDF_COLOR_PICKER,
    ) {
        PdfReaderColorPickerScreen(onBack = navigation::onBack)
    }
    pdfPlainReader(navigation)

}

internal fun NavGraphBuilder.pdfReaderNavScreensForPhone(
    onExportPdf: (file: File) -> Unit,
    navigation: ZoteroNavigation,
    navigateToTagPicker: () -> Unit,
) {
    pdfScreen(
        onBack = navigation::onBack,
        onExportPdf = onExportPdf,
        navigateToPdfFilter = navigation::toPdfFilterScreen,
        navigateToPdfSettings = navigation::toPdfSettings,
        navigateToPdfPlainReader = navigation::toPdfPlainReader,
        navigateToPdfAnnotation = navigation::toPdfAnnotationScreen,
        navigateToPdfAnnotationMore = navigation::toPdfAnnotationMoreScreen,
        navigateToPdfColorPicker = navigation::toPdfColorPicker,
        navigateToTagPicker = navigateToTagPicker,
        navigateToSingleCitationScreen = navigation::toSingleCitationScreen
    )
    pdfFilterNavScreens(navigation)
    pdfPlainReader(navigation)
    pdfColorPicker(navigation)

}

private fun NavGraphBuilder.pdfPlainReader(navigation: ZoteroNavigation) {
    composable(
        route = "${PdfReaderDestinations.PDF_PLAIN_READER}/{$ARG_PDF_PLAIN_READER_SCREEN}",
        arguments = listOf(
            navArgument(ARG_PDF_PLAIN_READER_SCREEN) { type = NavType.StringType },
        ),
    ) {
        PdfPlanReaderScreen(onBack = navigation::onBack)
    }
}

private fun NavGraphBuilder.pdfColorPicker(navigation: ZoteroNavigation) {
    composable(
        route = PdfReaderDestinations.PDF_COLOR_PICKER,
        arguments = listOf(),
    ) {
        PdfReaderColorPickerScreen(onBack = navigation::onBack)
    }
}



private fun NavGraphBuilder.pdfScreen(
    onBack: () -> Unit,
    onExportPdf: (file: File) -> Unit,
    navigateToPdfFilter: () -> Unit,
    navigateToPdfSettings: (args: String) -> Unit,
    navigateToPdfPlainReader: (args: String) -> Unit,
    navigateToPdfColorPicker: () -> Unit,
    navigateToPdfAnnotation: () -> Unit,
    navigateToPdfAnnotationMore: () -> Unit,
    navigateToTagPicker: () -> Unit,
    navigateToSingleCitationScreen: () -> Unit,
) {
    composable(
        route = "${PdfReaderDestinations.PDF_SCREEN}/{$ARG_PDF_SCREEN}",
        arguments = listOf(
            navArgument(ARG_PDF_SCREEN) { type = NavType.StringType },
        ),
    ) {
        PdfReaderScreen(
            onBack = onBack,
            onExportPdf = onExportPdf,
            navigateToPdfFilter = navigateToPdfFilter,
            navigateToPdfSettings = navigateToPdfSettings,
            navigateToPdfPlainReader = navigateToPdfPlainReader,
            navigateToPdfAnnotation = navigateToPdfAnnotation,
            navigateToPdfAnnotationMore = navigateToPdfAnnotationMore,
            navigateToPdfColorPicker = navigateToPdfColorPicker,
            navigateToTagPicker = navigateToTagPicker,
            navigateToSingleCitationScreen = navigateToSingleCitationScreen,
        )
    }
}

private object PdfReaderDestinations {
    const val PDF_FILTER_NAVIGATION = "pdfFilterNavigation"
    const val PDF_SCREEN = "pdfScreen"
    const val PDF_SETTINGS = "pdfSettings"
    const val PDF_PLAIN_READER = "pdfPlainReader"
    const val PDF_COLOR_PICKER = "pdfColorPicker"
    const val PDF_ANNOTATION_NAVIGATION = "pdfAnnotationNavigation"
    const val PDF_ANNOTATION_MORE_NAVIGATION = "pdfAnnotationMoreNavigation"
    const val SINGLE_CITATION_PICKER_DIALOG = "singleCitationPickerDialog"
}

fun ZoteroNavigation.toPdfScreen(
    context: Context,
    wasPspdfkitInitialized: Boolean,
    pdfScreenParams: String,
) {
    if (wasPspdfkitInitialized) {
        navController.navigate("${PdfReaderDestinations.PDF_SCREEN}/$pdfScreenParams")
    } else {
        context.longToast("Unable to open a PDF. PSPDFKIT was not initialized")
    }
}

private fun ZoteroNavigation.toPdfFilterNavigation() {
    navController.navigate(PdfReaderDestinations.PDF_FILTER_NAVIGATION)
}

private fun ZoteroNavigation.toPdfSettings(args: String) {
    navController.navigate("${PdfReaderDestinations.PDF_SETTINGS}/$args")
}

private fun ZoteroNavigation.toSingleCitationScreen() {
    navController.navigate("${PdfReaderDestinations.SINGLE_CITATION_PICKER_DIALOG}")
}

private fun ZoteroNavigation.toPdfPlainReader(args: String) {
    navController.navigate("${PdfReaderDestinations.PDF_PLAIN_READER}/$args")
}

private fun ZoteroNavigation.toPdfAnnotationNavigation() {
    navController.navigate(PdfReaderDestinations.PDF_ANNOTATION_NAVIGATION)
}

private fun ZoteroNavigation.toPdfAnnotationMoreNavigation() {
    navController.navigate(PdfReaderDestinations.PDF_ANNOTATION_MORE_NAVIGATION)
}

private fun ZoteroNavigation.toPdfColorPicker() {
    navController.navigate(PdfReaderDestinations.PDF_COLOR_PICKER)
}
