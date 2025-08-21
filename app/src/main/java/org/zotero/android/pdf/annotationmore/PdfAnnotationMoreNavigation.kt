package org.zotero.android.pdf.annotationmore

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.annotationmore.data.PdfAnnotationMoreArgs
import org.zotero.android.pdf.annotationmore.editpage.PdfAnnotationEditPageScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun PdfAnnotationMoreNavigation(args: PdfAnnotationMoreArgs, onBack: () -> Unit) {
    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val isTablet = layoutType.isTablet()
    if (!isTablet) {
        BackHandler(onBack = {
            onBack()
        })
    }

    ZoteroNavHost(
        navController = navController,
        startDestination = PdfAnnotationMoreDestination.PDF_ANNOTATION_MORE_SCREEN,
    ) {
        pdfAnnotationMoreNavScreens(args = args, navigation = navigation)
    }
}

internal fun NavGraphBuilder.pdfAnnotationMoreNavScreens(
    args: PdfAnnotationMoreArgs,
    navigation: ZoteroNavigation,
) {
    pdfAnnotationMoreScreen(
        args = args,
        onBack = navigation::onBack,
        navigateToPageEdit = navigation::toPageEdit
    )
    pageEditScreen(onBack = navigation::onBack)
}

private fun NavGraphBuilder.pdfAnnotationMoreScreen(
    args: PdfAnnotationMoreArgs,
    onBack: () -> Unit,
    navigateToPageEdit: () -> Unit,
) {
    composable(
        route = PdfAnnotationMoreDestination.PDF_ANNOTATION_MORE_SCREEN,
        arguments = listOf(),
    ) {
        PdfAnnotationMoreScreen(
            args = args,
            onBack = onBack,
            navigateToPageEdit = navigateToPageEdit
        )
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
