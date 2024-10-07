
package org.zotero.android.architecture.navigation.tablet

import android.net.Uri
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import org.zotero.android.architecture.EventBusConstants.FileWasSelected.CallPoint
import org.zotero.android.architecture.navigation.CommonScreenDestinations
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.allItemsScreen
import org.zotero.android.architecture.navigation.dialogDynamicHeight
import org.zotero.android.architecture.navigation.dialogFixedMaxHeight
import org.zotero.android.architecture.navigation.imageViewerScreen
import org.zotero.android.architecture.navigation.itemDetailsScreen
import org.zotero.android.architecture.navigation.loadingScreen
import org.zotero.android.architecture.navigation.phone.ARG_ADD_BY_IDENTIFIER
import org.zotero.android.architecture.navigation.toImageViewerScreen
import org.zotero.android.architecture.navigation.toItemDetails
import org.zotero.android.architecture.navigation.toVideoPlayerScreen
import org.zotero.android.architecture.navigation.videoPlayerScreen
import org.zotero.android.screens.collectionpicker.CollectionPickerScreen
import org.zotero.android.screens.creatoredit.CreatorEditNavigation
import org.zotero.android.screens.scanbarcode.ui.ScanBarcodeScreen
import org.zotero.android.screens.sortpicker.SortPickerNavigation
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.screens.addbyidentifier.ui.AddByIdentifierScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import org.zotero.android.uicomponents.singlepicker.SinglePickerScreen
import java.io.File

@Composable
internal fun TabletRightPaneNavigation(
    onPickFile: (callPoint: CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onShowPdf: (String) -> Unit,
    toAddOrEditNote: () -> Unit,
    toZoteroWebViewScreen: (String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    navController: NavHostController,
    navigation: ZoteroNavigation,
) {
    ZoteroNavHost(
        navController = navController,
        startDestination = CommonScreenDestinations.ALL_ITEMS,
        modifier = Modifier
            .navigationBarsPadding(), // do not draw behind nav bar
    ) {
        loadingScreen()
        allItemsScreen(
            onPickFile = { onPickFile(CallPoint.AllItems) },
            onOpenFile = onOpenFile,
            onOpenWebpage = onOpenWebpage,
            navigateToCollectionsScreen = {},//no-op
            navigateToItemDetails = navigation::toItemDetails,
            navigateToAddOrEditNote = toAddOrEditNote,
            navigateToSinglePicker = navigation::toSinglePickerDialog,
            navigateToAllItemsSort = navigation::toAllItemsSortDialog,
            navigateToAddByIdentifier = navigation::toAddByIdentifierDialog,
            navigateToVideoPlayerScreen = navigation::toVideoPlayerScreen,
            navigateToImageViewerScreen = navigation::toImageViewerScreen,
            navigateToZoterWebViewScreen = toZoteroWebViewScreen,
            navigateToTagFilter = { },
            navigateToCollectionPicker = navigation::toCollectionPickerDialog,
            navigateToScanBarcode = navigation::toScanBarcodeDialog,
            onShowPdf = onShowPdf,
        )
        itemDetailsScreen(
            navigateToCreatorEdit = navigation::toCreatorEditDialog,
            navigateToTagPicker = navigation::toTagPickerDialog,
            navigateToSinglePicker = navigation::toSinglePickerDialog,
            navigateToAddOrEditNote = toAddOrEditNote,
            navigateToVideoPlayerScreen = navigation::toVideoPlayerScreen,
            navigateToImageViewerScreen = navigation::toImageViewerScreen,
            navigateToZoterWebViewScreen = toZoteroWebViewScreen,
            onBack = navigation::onBack,
            onOpenFile = onOpenFile,
            onOpenWebpage = onOpenWebpage,
            onPickFile = { onPickFile(CallPoint.ItemDetails) },
            onShowPdf = onShowPdf,
        )
        videoPlayerScreen()
        imageViewerScreen(onBack = navigation::onBack)
        dialogDynamicHeight(
            route = TabletRightPaneDestinations.TAG_PICKER_DIALOG,
        ) {
            TagPickerScreen(onBack = navigation::onBack)
        }

        dialogDynamicHeight(
            route = TabletRightPaneDestinations.COLLECTION_PICKER_DIALOG,
        ) {
            CollectionPickerScreen(onBack = { navController.popBackStack() })
        }

        dialogFixedMaxHeight(
            route = TabletRightPaneDestinations.ALL_ITEMS_SORT_DIALOG,
        ) {
            SortPickerNavigation()
        }
        dialogFixedMaxHeight(
            route = TabletRightPaneDestinations.CREATOR_EDIT_DIALOG,
        ) {
            CreatorEditNavigation()
        }
        dialogDynamicHeight(
            route = TabletRightPaneDestinations.SINGLE_PICKER_DIALOG,
        ) {
            SinglePickerScreen(
                onCloseClicked = { navController.popBackStack() }
            )
        }

        dialogFixedMaxHeight(
            route = "${TabletRightPaneDestinations.ADD_BY_IDENTIFIER_DIALOG}/{$ARG_ADD_BY_IDENTIFIER}",
            arguments = listOf(
                navArgument(ARG_ADD_BY_IDENTIFIER) { type = NavType.StringType },
            ),
        ) {
            AddByIdentifierScreen(
                onClose = {
                    navController.popBackStack()
                },
            )
        }

        dialogFixedMaxHeight(
            route = TabletRightPaneDestinations.SCAN_BARCODE_DIALOG,
            arguments = listOf(),
        ) {
            ScanBarcodeScreen(
                onClose = {
                    navController.popBackStack()
                },
            )
        }
    }
}

private object TabletRightPaneDestinations {
    const val ALL_ITEMS_SORT_DIALOG = "allItemsSortDialog"
    const val CREATOR_EDIT_DIALOG = "creatorEditDialog"
    const val SINGLE_PICKER_DIALOG = "singlePickerDialog"
    const val ADD_BY_IDENTIFIER_DIALOG = "addByIdentifierDialog"
    const val TAG_PICKER_DIALOG = "tagPickerDialog"
    const val COLLECTION_PICKER_DIALOG = "collectionPickerDialog"
    const val SCAN_BARCODE_DIALOG = "scanBarcodeDialog"

}

private fun ZoteroNavigation.toAllItemsSortDialog() {
    navController.navigate(TabletRightPaneDestinations.ALL_ITEMS_SORT_DIALOG)
}

private fun ZoteroNavigation.toCreatorEditDialog() {
    navController.navigate(TabletRightPaneDestinations.CREATOR_EDIT_DIALOG)
}

private fun ZoteroNavigation.toTagPickerDialog() {
    navController.navigate(TabletRightPaneDestinations.TAG_PICKER_DIALOG)
}

private fun ZoteroNavigation.toSinglePickerDialog() {
    navController.navigate(TabletRightPaneDestinations.SINGLE_PICKER_DIALOG)
}

private fun ZoteroNavigation.toAddByIdentifierDialog(params: String) {
    navController.navigate("${TabletRightPaneDestinations.ADD_BY_IDENTIFIER_DIALOG}/$params")
}

private fun ZoteroNavigation.toCollectionPickerDialog() {
    navController.navigate(TabletRightPaneDestinations.COLLECTION_PICKER_DIALOG)
}

private fun ZoteroNavigation.toScanBarcodeDialog() {
    navController.navigate(TabletRightPaneDestinations.SCAN_BARCODE_DIALOG)
}