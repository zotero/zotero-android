package org.zotero.android.architecture.navigation.phone

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.zotero.android.architecture.Consumable
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.navigation.ARG_RETRIEVE_METADATA
import org.zotero.android.architecture.navigation.ARG_TAGS_FILTER
import org.zotero.android.architecture.navigation.CommonScreenDestinations
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.addNoteScreen
import org.zotero.android.architecture.navigation.allItemsScreen
import org.zotero.android.architecture.navigation.collectionsScreen
import org.zotero.android.architecture.navigation.imageViewerScreen
import org.zotero.android.architecture.navigation.itemDetailsScreen
import org.zotero.android.architecture.navigation.librariesScreen
import org.zotero.android.architecture.navigation.loadingScreen
import org.zotero.android.architecture.navigation.toAddOrEditNote
import org.zotero.android.architecture.navigation.toImageViewerScreen
import org.zotero.android.architecture.navigation.toItemDetails
import org.zotero.android.architecture.navigation.toVideoPlayerScreen
import org.zotero.android.architecture.navigation.toZoteroWebViewScreen
import org.zotero.android.architecture.navigation.videoPlayerScreen
import org.zotero.android.architecture.navigation.zoterWebViewScreen
import org.zotero.android.pdf.pdfReaderNavScreensForPhone
import org.zotero.android.pdf.toPdfScreen
import org.zotero.android.screens.addbyidentifier.ui.AddByIdentifierScreen
import org.zotero.android.screens.citation.singlecitation.SingleCitationScreen
import org.zotero.android.screens.collectionedit.collectionEditNavScreens
import org.zotero.android.screens.collectionedit.toCollectionEditScreen
import org.zotero.android.screens.collectionpicker.CollectionPickerScreen
import org.zotero.android.screens.creatoredit.creatorEditNavScreens
import org.zotero.android.screens.creatoredit.toCreatorEdit
import org.zotero.android.screens.dashboard.DashboardViewEffect
import org.zotero.android.screens.filter.FilterScreenPhone
import org.zotero.android.screens.retrievemetadata.RetrieveMetadataScreen
import org.zotero.android.screens.scanbarcode.ui.ScanBarcodeScreen
import org.zotero.android.screens.settings.settingsNavScreens
import org.zotero.android.screens.settings.toSettingsScreen
import org.zotero.android.screens.sortpicker.sortPickerNavScreens
import org.zotero.android.screens.sortpicker.toSortPicker
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import org.zotero.android.uicomponents.singlepicker.SinglePickerScreen
import org.zotero.android.uicomponents.theme.CustomTheme
import java.io.File

internal const val ARG_ADD_BY_IDENTIFIER = "addByIdentifierArg"

@Composable
internal fun DashboardRootPhoneNavigation(
    collectionDefaultValue: String,
    onPickFile: (callPoint: EventBusConstants.FileWasSelected.CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    onExportPdf: (file: File) -> Unit,
    wasPspdfkitInitialized: Boolean,
    viewEffect: Consumable<DashboardViewEffect>?
) {

    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }


    LaunchedEffect(key1 = viewEffect) {
        val consumedEffect = viewEffect?.consume()
        when (consumedEffect) {
            null -> Unit
            is DashboardViewEffect.NavigateToCollectionsScreen -> navigateToCollectionsScreen(
                navController, consumedEffect.screenArgs
            )
        }
    }

    val context = LocalContext.current

    Column(modifier = Modifier.background(color = CustomTheme.colors.surface)) {
        ZoteroNavHost(
            navController = navController,
            startDestination = CommonScreenDestinations.ALL_ITEMS,
            modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
        ) {
            collectionsScreen(
                collectionDefaultValue = collectionDefaultValue,
                onBack = navigation::onBack,
                navigateToAllItems = {
                    toAllItems(
                        navController = navController,it
                    )
                },
                navigateToLibraries = {
                    navController.navigate(CommonScreenDestinations.LIBRARIES_SCREEN)
                },
                navigateToCollectionEdit = { navigation.toCollectionEditScreen() },
            )
            librariesScreen(
                navigateToCollectionsScreen = {
                    navigateToCollectionsScreen(navController, it)
                },
                onSettingsTapped = { navigation.toSettingsScreen() }
            )

            loadingScreen()
            allItemsScreen(
                onPickFile = { onPickFile(EventBusConstants.FileWasSelected.CallPoint.AllItems) },
                onOpenFile = onOpenFile,
                onOpenWebpage = onOpenWebpage,
                navigateToCollectionsScreen = navigation::toCollectionsScreen,
                navigateToItemDetails = navigation::toItemDetails,
                navigateToAddOrEditNote = navigation::toAddOrEditNote,
                navigateToSinglePicker = navigation::toSinglePicker,
                navigateToAllItemsSort = navigation::toSortPicker,
                navigateToVideoPlayerScreen = navigation::toVideoPlayerScreen,
                navigateToImageViewerScreen = navigation::toImageViewerScreen,
                navigateToZoterWebViewScreen = navigation::toZoteroWebViewScreen,
                navigateToRetrieveMetadata = navigation::toRetrieveMetadata,
                navigateToTagFilter = navigation::toTagFilter,
                navigateToAddByIdentifier = navigation::toAddByIdentifier,
                navigateToCollectionPicker = navigation::toCollectionPicker,
                navigateToScanBarcode = navigation::toScanBarcode,
                navigateToSingleCitation = navigation::toSingleCitation,
                onShowPdf = { pdfScreenParams ->
                    navigation.toPdfScreen(
                        context = context,
                        pdfScreenParams = pdfScreenParams,
                        wasPspdfkitInitialized = wasPspdfkitInitialized
                    )
                },
            )
            itemDetailsScreen(
                navigateToCreatorEdit = navigation::toCreatorEdit,
                navigateToTagPicker = navigation::toTagPicker,
                navigateToSinglePicker = navigation::toSinglePicker,
                navigateToAddOrEditNote = navigation::toAddOrEditNote,
                navigateToVideoPlayerScreen = navigation::toVideoPlayerScreen,
                navigateToImageViewerScreen = navigation::toImageViewerScreen,
                navigateToZoterWebViewScreen = navigation::toZoteroWebViewScreen,
                onBack = navigation::onBack,
                onOpenFile = onOpenFile,
                onOpenWebpage = onOpenWebpage,
                onPickFile = { onPickFile(EventBusConstants.FileWasSelected.CallPoint.ItemDetails) },
                onShowPdf = { pdfScreenParams ->
                    navigation.toPdfScreen(
                        context = context,
                        wasPspdfkitInitialized = wasPspdfkitInitialized,
                        pdfScreenParams = pdfScreenParams
                    )
                },
            )

            composable(
                route = DashboardRootPhoneDestinations.TAG_PICKER,
                arguments = listOf(),
            ) {
                TagPickerScreen(onBack = navigation::onBack)
            }

            composable(
                route = "${DashboardRootPhoneDestinations.RETRIEVE_METADATA}/{$ARG_RETRIEVE_METADATA}",
                arguments = listOf(
                    navArgument(ARG_RETRIEVE_METADATA) { type = NavType.StringType },
                ),
            ) {
                RetrieveMetadataScreen(onBack = navigation::onBack)
            }

            composable(
                route = "${DashboardRootPhoneDestinations.TAG_FILTER}/{$ARG_TAGS_FILTER}",
                arguments = listOf(
                    navArgument(ARG_TAGS_FILTER) { type = NavType.StringType },
                ),
            ) {
                FilterScreenPhone(onBack = navigation::onBack)
            }

            composable(
                route = DashboardRootPhoneDestinations.SINGLE_PICKER,
                arguments = listOf(),
            ) {
                SinglePickerScreen(
                    onCloseClicked = navigation::onBack,
                )
            }

            composable(
                route = "${DashboardRootPhoneDestinations.ADD_BY_IDENTIFIER}/{$ARG_ADD_BY_IDENTIFIER}",
                arguments = listOf(
                    navArgument(ARG_ADD_BY_IDENTIFIER) { type = NavType.StringType },
                ),
            ) {
                AddByIdentifierScreen(
                    onClose = navigation::onBack,
                )
            }

            composable(
                route = DashboardRootPhoneDestinations.SCAN_BARCODE,
                arguments = listOf(),
            ) {
                ScanBarcodeScreen(
                    onClose = navigation::onBack,
                )
            }

            composable(
                route = DashboardRootPhoneDestinations.COLLECTION_PICKER,
                arguments = listOf(),
            ) {
                CollectionPickerScreen(onBack = navigation::onBack)
            }

            sortPickerNavScreens(navigation)
            creatorEditNavScreens(navigation)
            collectionEditNavScreens(navigation)
            settingsNavScreens(navigation = navigation, onOpenWebpage = onOpenWebpage)

            videoPlayerScreen()
            imageViewerScreen(onBack = navigation::onBack)

            pdfReaderNavScreensForPhone(
                onExportPdf = onExportPdf,
                navigation = navigation,
                navigateToTagPicker = navigation::toTagPicker
            )
            addNoteScreen(
                onBack = navigation::onBack,
                navigateToTagPicker = navigation::toTagPicker
            )
            zoterWebViewScreen(onClose = navigation::onBack)

            composable(
                route = DashboardRootPhoneDestinations.SINGLE_CITATION,
                arguments = listOf(),
            ) {
                SingleCitationScreen(onBack = navigation::onBack)
            }
        }
    }
}

private fun navigateToCollectionsScreen(navController: NavHostController, collectionArgs: String) {
    navController.popBackStack(navController.graph.id, inclusive = true)
    navController.navigate(CommonScreenDestinations.LIBRARIES_SCREEN)
    navController.navigate("${CommonScreenDestinations.COLLECTIONS_SCREEN}/$collectionArgs")
}

private object DashboardRootPhoneDestinations {
    const val SINGLE_PICKER = "singlePicker"
    const val ADD_BY_IDENTIFIER = "addByIdentifier"
    const val TAG_PICKER = "tagPicker"
    const val TAG_FILTER = "tagFilter"
    const val COLLECTION_PICKER = "collectionPicker"
    const val SCAN_BARCODE = "scanBarcode"
    const val RETRIEVE_METADATA = "retrieveMetadata"
    const val SINGLE_CITATION = "singleCitation"

}

private fun ZoteroNavigation.toCollectionsScreen(params: String) {
    navController.navigate("${CommonScreenDestinations.COLLECTIONS_SCREEN}/$params") {
        launchSingleTop = true
    }
}

private fun ZoteroNavigation.toTagPicker() {
    navController.navigate(DashboardRootPhoneDestinations.TAG_PICKER)
}

private fun ZoteroNavigation.toSinglePicker() {
    navController.navigate(DashboardRootPhoneDestinations.SINGLE_PICKER)
}

private fun ZoteroNavigation.toAddByIdentifier(params: String) {
    navController.navigate("${DashboardRootPhoneDestinations.ADD_BY_IDENTIFIER}/$params")
}

private fun ZoteroNavigation.toCollectionPicker() {
    navController.navigate(DashboardRootPhoneDestinations.COLLECTION_PICKER)
}

private fun ZoteroNavigation.toTagFilter(params: String) {
    navController.navigate("${DashboardRootPhoneDestinations.TAG_FILTER}/$params")
}

private fun ZoteroNavigation.toRetrieveMetadata(args: String) {
    navController.navigate("${DashboardRootPhoneDestinations.RETRIEVE_METADATA}/$args")
}

private fun ZoteroNavigation.toSingleCitation() {
    navController.navigate(DashboardRootPhoneDestinations.SINGLE_CITATION)
}

private fun toAllItems(
    navController: NavHostController,
    collectionArgs: String,
) {
    navController.popBackStack(navController.graph.id, inclusive = true)
    navController.navigate("${CommonScreenDestinations.COLLECTIONS_SCREEN}/$collectionArgs")
    navController.navigate(CommonScreenDestinations.ALL_ITEMS)
}

private fun ZoteroNavigation.toScanBarcode() {
    navController.navigate(DashboardRootPhoneDestinations.SCAN_BARCODE)
}

