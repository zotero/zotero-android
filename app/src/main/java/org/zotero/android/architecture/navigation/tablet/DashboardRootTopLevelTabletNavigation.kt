
package org.zotero.android.architecture.navigation.tablet

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.addNoteScreen
import org.zotero.android.architecture.navigation.toAddOrEditNote
import org.zotero.android.pdf.pdfReaderScreenAndNavigation
import org.zotero.android.pdf.toPdfScreen
import org.zotero.android.screens.dashboard.DashboardViewModel
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import java.io.File

@Composable
internal fun DashboardRootTopLevelTabletNavigation(
    onPickFile: (callPoint: EventBusConstants.FileWasSelected.CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    viewModel: DashboardViewModel,
) {
    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }

    ZoteroNavHost(
        navController = navController,
        startDestination = DashboardRootDestinations.DASHBOARD_SCREEN,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        dashboardScreen(
            onPickFile = onPickFile,
            onOpenFile = onOpenFile,
            onOpenWebpage = onOpenWebpage,
            viewModel = viewModel,
            onShowPdf = navigation::toPdfScreen,
            toAddOrEditNote = navigation::toAddOrEditNote
        )
        pdfReaderScreenAndNavigation(navigation)
        addNoteScreen(
            onBack = navigation::onBack,
            navigateToTagPicker = navigation::toTagPicker
        )
        tagPickerScreen(onBack = navigation::onBack)
    }
}

private fun NavGraphBuilder.dashboardScreen(
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
        DashboardRootTabletNavigationScreen(
            onPickFile = onPickFile,
            onOpenFile = onOpenFile,
            onShowPdf = onShowPdf,
            toAddOrEditNote = toAddOrEditNote,
            onOpenWebpage = onOpenWebpage,
            viewModel = viewModel
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
    const val TAG_PICKER_SCREEN = "tagPickerScreen"
}

private fun ZoteroNavigation.toTagPicker() {
    navController.navigate(DashboardRootDestinations.TAG_PICKER_SCREEN)
}