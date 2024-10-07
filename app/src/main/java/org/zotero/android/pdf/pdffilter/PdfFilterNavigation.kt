package org.zotero.android.pdf.pdffilter

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun PdfFilterNavigation() {
    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    ZoteroNavHost(
        navController = navController,
        startDestination = PdfFilterDestinations.PDF_FILTER_SCREEN,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        pdfFilterNavScreens(navigation = navigation)
    }
}

internal fun NavGraphBuilder.pdfFilterNavScreens(
    navigation: ZoteroNavigation,
) {
    pdfFilterScreen(onBack = navigation::onBack, navigateToTagPicker = navigation::toTagPicker)
    tagPickerScreen(onBack = navigation::onBack)
}

private fun NavGraphBuilder.pdfFilterScreen(
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
) {
    composable(
        route = PdfFilterDestinations.PDF_FILTER_SCREEN,
    ) {
        PdfFilterScreen(onBack = onBack, navigateToTagPicker = navigateToTagPicker)
    }
}

private fun NavGraphBuilder.tagPickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = PdfFilterDestinations.TAG_PICKER_SCREEN,
    ) {
        TagPickerScreen(onBack = onBack)
    }
}

private object PdfFilterDestinations {
    const val PDF_FILTER_SCREEN = "pdfFilterScreen"
    const val TAG_PICKER_SCREEN = "tagPickerScreen"
}

fun ZoteroNavigation.toPdfFilterScreen() {
    navController.navigate(PdfFilterDestinations.PDF_FILTER_SCREEN)
}

private fun ZoteroNavigation.toTagPicker() {
    navController.navigate(PdfFilterDestinations.TAG_PICKER_SCREEN)
}
