
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
import org.zotero.android.screens.addnote.AddNoteScreen
import org.zotero.android.screens.tagpicker.TagPickerScreen
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
            toAddOrEditNote = navigation::toAddOrEditNote,
            onOpenWebpage = onOpenWebpage,
            viewModel = viewModel
        )
        pdfNavigation()
        addNoteScreen(
            onBack = navigation::onBack,
            navigateToTagPicker = navigation::toTagPicker
        )
        tagPickerScreen(onBack = navigation::onBack)
    }
}

private fun NavGraphBuilder.dashboardScreen(
    onBack: () -> Unit,
    onPickFile: (callPoint: EventBusConstants.FileWasSelected.CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onShowPdf: () -> Unit,
    toAddOrEditNote: () -> Unit,
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
            toAddOrEditNote = toAddOrEditNote,
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

private fun NavGraphBuilder.addNoteScreen(
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
) {
    composable(
        route = DashboardRootDestinations.ADD_NOTE,
        arguments = listOf(),
    ) {
        AddNoteScreen(
            onBack = onBack,
            navigateToTagPicker = navigateToTagPicker,
        )
    }
}

private fun NavGraphBuilder.tagPickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = DashboardRootDestinations.TAG_PICKER_SCREEN,
        arguments = listOf(),
    ) {
        TagPickerScreen(onBack = onBack)
    }
}

private object DashboardRootDestinations {
    const val DASHBOARD_SCREEN = "dashboardScreen"
    const val PDF_SCREEN = "pdfScreen"
    const val ADD_NOTE = "addNote"
    const val TAG_PICKER_SCREEN = "tagPickerScreen"

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
    fun toAddOrEditNote() {
        navController.navigate(DashboardRootDestinations.ADD_NOTE)
    }

    fun toTagPicker() {
        navController.navigate(DashboardRootDestinations.TAG_PICKER_SCREEN)
    }
}
