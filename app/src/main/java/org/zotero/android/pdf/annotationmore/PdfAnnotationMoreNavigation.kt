package org.zotero.android.pdf.annotationmore

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.pdf.annotationmore.editpage.PdfAnnotationEditPageScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun PdfAnnotationMoreNavigation() {
    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    ZoteroNavHost(
        navController = navController,
        startDestination = PdfAnnotationMoreDestination.PDF_ANNOTATION_MORE_SCREEN,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        pdfAnnotationMoreNavScreens(navigation = navigation)
    }
}

internal fun NavGraphBuilder.pdfAnnotationMoreNavScreens(
    navigation: ZoteroNavigation,
) {
    pdfAnnotationMoreScreen(onBack = navigation::onBack, navigateToPageEdit = navigation::toPageEdit)
    pageEditScreen(onBack = navigation::onBack)
}

private fun NavGraphBuilder.pdfAnnotationMoreScreen(
    onBack: () -> Unit,
    navigateToPageEdit: () -> Unit,
) {
    composable(
        route = PdfAnnotationMoreDestination.PDF_ANNOTATION_MORE_SCREEN,
        arguments = listOf(),
    ) {
        PdfAnnotationMoreScreen(onBack = onBack, navigateToPageEdit = navigateToPageEdit)
    }
}

private fun NavGraphBuilder.pageEditScreen(
    onBack: () -> Unit,
) {
    composable(
        route = PdfAnnotationMoreDestination.PAGE_EDIT_SCREEN,
    ) {
        PdfAnnotationEditPageScreen(onBack = onBack)
    }
}

private object PdfAnnotationMoreDestination {
    const val PDF_ANNOTATION_MORE_SCREEN = "pdfAnnotationMoreScreen"
    const val PAGE_EDIT_SCREEN = "pageEditScreen"
}

fun ZoteroNavigation.toPdfAnnotationMoreScreen() {
    navController.navigate(PdfAnnotationMoreDestination.PDF_ANNOTATION_MORE_SCREEN)
}

private fun ZoteroNavigation.toPageEdit() {
    navController.navigate(PdfAnnotationMoreDestination.PAGE_EDIT_SCREEN)
}
