
package org.zotero.android.architecture.navigation.tablet

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.zotero.android.architecture.Consumable
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.navigation.ARG_RETRIEVE_METADATA
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.addNoteScreen
import org.zotero.android.architecture.navigation.dialogDynamicHeight
import org.zotero.android.architecture.navigation.dialogFixedMaxHeight
import org.zotero.android.architecture.navigation.toAddOrEditNote
import org.zotero.android.architecture.navigation.toZoteroWebViewScreen
import org.zotero.android.architecture.navigation.zoterWebViewScreen
import org.zotero.android.pdf.pdfReaderScreenAndNavigationForTablet
import org.zotero.android.pdf.toPdfScreen
import org.zotero.android.screens.dashboard.DashboardViewEffect
import org.zotero.android.screens.retrievemetadata.RetrieveMetadataScreen
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import java.io.File

@Composable
internal fun DashboardRootTopLevelTabletNavigation(
    onPickFile: (callPoint: EventBusConstants.FileWasSelected.CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    wasPspdfkitInitialized: Boolean,
    viewEffect: Consumable<DashboardViewEffect>?,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
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
            viewEffect = viewEffect,
            onShowPdf = { pdfScreenParams ->
                navigation.toPdfScreen(
                    context = context,
                    wasPspdfkitInitialized = wasPspdfkitInitialized,
                    pdfScreenParams = pdfScreenParams
                )
            },
            toAddOrEditNote = navigation::toAddOrEditNote,
            toZoteroWebViewScreen = navigation::toZoteroWebViewScreen,
            navigateToRetrieveMetadata = navigation::toRetrieveMetadata
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
        retrieveMetadataDialog(onBack = {
            navController.popBackStack()
        })
    }
}

private fun NavGraphBuilder.dashboardScreen(
    onPickFile: (callPoint: EventBusConstants.FileWasSelected.CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onShowPdf: (String) -> Unit,
    toAddOrEditNote: (String) -> Unit,
    toZoteroWebViewScreen: (String) -> Unit,
    navigateToRetrieveMetadata: (params: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    viewEffect: Consumable<DashboardViewEffect>?,
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
            navigateToRetrieveMetadata = navigateToRetrieveMetadata,
            onOpenWebpage = onOpenWebpage,
            viewEffect = viewEffect
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

private fun NavGraphBuilder.retrieveMetadataDialog(onBack: () -> Unit) {
    dialogFixedMaxHeight(
        route = "${DashboardRootDestinations.RETRIEVE_METADATA_DIALOG}/{$ARG_RETRIEVE_METADATA}",
        arguments = listOf(
            navArgument(ARG_RETRIEVE_METADATA) { type = NavType.StringType },
        ),
    ) {
        RetrieveMetadataScreen(onBack = onBack)
    }
}


private object DashboardRootDestinations {
    const val DASHBOARD_SCREEN = "dashboardScreen"
    const val TAG_PICKER_SCREEN = "tagPickerScreen"
    const val TAG_PICKER_DIALOG = "tagPickerDialog"
    const val RETRIEVE_METADATA_DIALOG = "retrieveMetadataDialog"
}

private fun ZoteroNavigation.toTagPickerScreen() {
    navController.navigate(DashboardRootDestinations.TAG_PICKER_SCREEN)
}

private fun ZoteroNavigation.toTagPickerDialog() {
    navController.navigate(DashboardRootDestinations.TAG_PICKER_DIALOG)
}

private fun ZoteroNavigation.toRetrieveMetadata(args: String) {
    navController.navigate("${DashboardRootDestinations.RETRIEVE_METADATA_DIALOG}/$args")
}