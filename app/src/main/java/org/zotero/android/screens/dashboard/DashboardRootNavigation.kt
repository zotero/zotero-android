
package org.zotero.android.screens.dashboard

import android.net.Uri
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.PdfReaderNavigation
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import java.io.File

@Composable
internal fun DashboardRootNavigation(
    onPickFile: (callPoint: EventBusConstants.FileWasSelected.CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    viewModel: DashboardViewModel,
) {
    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        DashboardRootNavigation(navController, dispatcher)
    }

    val layoutType = CustomLayoutSize.calculateLayoutType()
    ZoteroNavHost(
        navController = navController,
        startDestination = DashboardRootDestinations.DASHBOARD_SCREEN,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        dashboardScreen(
            onBack = navigation::onBack,
            onPickFile = onPickFile,
            onOpenFile = onOpenFile,
            onShowPdf = navigation::toPdfScreen,
            onOpenWebpage = onOpenWebpage,
            viewModel = viewModel
        )
        pdfNavigation()
    }
}

private fun NavGraphBuilder.dashboardScreen(
    onBack: () -> Unit,
    onPickFile: (callPoint: EventBusConstants.FileWasSelected.CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onShowPdf: () -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    viewModel: DashboardViewModel,
) {
    composable(
        route = DashboardRootDestinations.DASHBOARD_SCREEN,
        arguments = listOf(),
    ) {
        DashboardScreen(
            onBack = onBack,
            onPickFile = onPickFile,
            onOpenFile = onOpenFile,
            onShowPdf = onShowPdf,
            onOpenWebpage = onOpenWebpage,
            viewModel = viewModel
        )
    }
}

private fun NavGraphBuilder.pdfNavigation(
) {
    composable(
        route = DashboardRootDestinations.PDF_SCREEN,
        arguments = listOf(),
        enterTransition = { slideInVertically() },
    ) {
        PdfReaderNavigation()
    }
}

private object DashboardRootDestinations {
    const val DASHBOARD_SCREEN = "dashboardScreen"
    const val PDF_SCREEN = "pdfScreen"
}

@SuppressWarnings("UseDataClass")
private class DashboardRootNavigation(
    private val navController: NavHostController,
    private val onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    fun onBack() = onBackPressedDispatcher?.onBackPressed()

    fun toPdfScreen() {
        navController.navigate(DashboardRootDestinations.PDF_SCREEN)
    }
}
