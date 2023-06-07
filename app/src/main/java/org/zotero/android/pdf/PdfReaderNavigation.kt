
package org.zotero.android.pdf

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.screenOrDialogFixedDimens
import org.zotero.android.pdffilter.PdfFilterNavigation
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun PdfReaderNavigation() {
    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        PdfReaderNavigation(navController, dispatcher)
    }

    val layoutType = CustomLayoutSize.calculateLayoutType()
    ZoteroNavHost(
        navController = navController,
        startDestination = PdfReaderDestinations.PDF_SCREEN,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        pdfScreen(onBack = navigation::onBack, navigateToPdfFilter = navigation::toPdfFilter)
        screenOrDialogFixedDimens(
            modifier = Modifier.height(400.dp).width(400.dp),
            route = PdfReaderDestinations.PDF_FILTER_NAVIGATION,
            layoutType = layoutType,
        ) {
            PdfFilterNavigation()
        }
    }
}

private fun NavGraphBuilder.pdfScreen(
    onBack: () -> Unit,
    navigateToPdfFilter: () -> Unit,
) {
    composable(
        route = PdfReaderDestinations.PDF_SCREEN,
        arguments = listOf(),
    ) {
        PdfReaderScreen(onBack = { onBack() }, navigateToPdfFilter = navigateToPdfFilter)
    }
}

private object PdfReaderDestinations {
    const val PDF_SCREEN = "pdfScreen"
    const val PDF_FILTER_NAVIGATION = "pdfFilterNavigation"
}

@SuppressWarnings("UseDataClass")
private class PdfReaderNavigation(
    private val navController: NavHostController,
    private val onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    fun onBack() = onBackPressedDispatcher?.onBackPressed()

    fun toPdfFilter() {
        navController.navigate(PdfReaderDestinations.PDF_FILTER_NAVIGATION)
    }
}
