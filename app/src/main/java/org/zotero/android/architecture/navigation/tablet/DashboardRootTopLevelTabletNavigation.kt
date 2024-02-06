
package org.zotero.android.architecture.navigation.tablet

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.addNoteScreen
import org.zotero.android.architecture.navigation.dialogDynamicHeight
import org.zotero.android.architecture.navigation.toAddOrEditNote
import org.zotero.android.architecture.navigation.toZoteroWebViewScreen
import org.zotero.android.architecture.navigation.zoterWebViewScreen
import org.zotero.android.pdf.pdfReaderScreenAndNavigationForTablet
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
    wasPspdfkitInitialized: Boolean,
) {
    val context = LocalContext.current
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
            onShowPdf = { navigation.toPdfScreen(context, wasPspdfkitInitialized) },
            toAddOrEditNote = navigation::toAddOrEditNote,
            toZoteroWebViewScreen = navigation::toZoteroWebViewScreen,
        )
        pdfReaderScreenAndNavigationForTablet(
            navigation = navigation,
            navigateToTagPickerDialog = navigation::toTagPickerDialog
        )
        tagPickerScreen(onBack = navigation::onBack)
        tagPickerDialog(onBack = navigation::onBack)
        addNoteScreen(
            onBack = navigation::onBack,
            navigateToTagPicker = navigation::toTagPickerScreen
        )
        zoterWebViewScreen(onClose = navigation::onBack)
    }
}

private fun NavGraphBuilder.dashboardScreen(
    onPickFile: (callPoint: EventBusConstants.FileWasSelected.CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onShowPdf: () -> Unit,
    toAddOrEditNote: () -> Unit,
    toZoteroWebViewScreen: (String) -> Unit,
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
            toZoteroWebViewScreen = toZoteroWebViewScreen,
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

private fun NavGraphBuilder.tagPickerDialog(
    onBack: () -> Unit,
) {
    dialogDynamicHeight(
        route = DashboardRootDestinations.TAG_PICKER_DIALOG,
    ) {
        TagPickerScreen(onBack = onBack)
    }
}

private object DashboardRootDestinations {
    const val DASHBOARD_SCREEN = "dashboardScreen"
    const val TAG_PICKER_SCREEN = "tagPickerScreen"
    const val TAG_PICKER_DIALOG = "tagPickerDialog"
}

private fun ZoteroNavigation.toTagPickerScreen() {
    navController.navigate(DashboardRootDestinations.TAG_PICKER_SCREEN)
}

private fun ZoteroNavigation.toTagPickerDialog() {
    navController.navigate(DashboardRootDestinations.TAG_PICKER_DIALOG)
}