
package org.zotero.android.architecture.navigation.tablet

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.google.accompanist.insets.navigationBarsPadding
import org.zotero.android.architecture.EventBusConstants.FileWasSelected.CallPoint
import org.zotero.android.architecture.navigation.CommonScreenDestinations
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.allItemsScreen
import org.zotero.android.architecture.navigation.dialogDynamicHeight
import org.zotero.android.architecture.navigation.dialogFixedMaxHeight
import org.zotero.android.architecture.navigation.imageViewerScreen
import org.zotero.android.architecture.navigation.itemDetailsScreen
import org.zotero.android.architecture.navigation.loadingScreen
import org.zotero.android.architecture.navigation.toImageViewerScreen
import org.zotero.android.architecture.navigation.toItemDetails
import org.zotero.android.architecture.navigation.toVideoPlayerScreen
import org.zotero.android.architecture.navigation.videoPlayerScreen
import org.zotero.android.screens.creatoredit.CreatorEditNavigation
import org.zotero.android.screens.filter.FilterScreen
import org.zotero.android.screens.sortpicker.SortPickerNavigation
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import org.zotero.android.uicomponents.singlepicker.SinglePickerScreen
import java.io.File

@Composable
internal fun TabletRightPaneNavigation(
    onPickFile: (callPoint: CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onShowPdf: () -> Unit,
    toAddOrEditNote: () -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    navController: NavHostController,
    navigation: ZoteroNavigation,
) {
    ZoteroNavHost(
        navController = navController,
        startDestination = CommonScreenDestinations.ALL_ITEMS,
        modifier = Modifier
//            .fillMaxSize()
            .navigationBarsPadding(), // do not draw behind nav bar
//        enterTransition = { EnterTransition.None},
//        popEnterTransition = { EnterTransition.None},
//        popExitTransition = { ExitTransition.None},
//        exitTransition = { ExitTransition.None},
    ) {
        loadingScreen()
        allItemsScreen(
            onBack = navigation::onBack,
            onPickFile = { onPickFile(CallPoint.AllItems) },
            onOpenFile = onOpenFile,
            onOpenWebpage = onOpenWebpage,
            navigateToCollectionsScreen = {},//no-op
            navigateToItemDetails = navigation::toItemDetails,
            navigateToAddOrEditNote = toAddOrEditNote,
            navigateToSinglePicker = navigation::toSinglePickerDialog,
            navigateToAllItemsSort = navigation::toAllItemsSortDialog,
            navigateToVideoPlayerScreen = navigation::toVideoPlayerScreen,
            navigateToImageViewerScreen = navigation::toImageViewerScreen,
            navigateToTagFilter = navigation::toTagFilter,
            onShowPdf = onShowPdf,
        )
        itemDetailsScreen(
            navigateToCreatorEdit = navigation::toCreatorEditDialog,
            navigateToTagPicker = navigation::toTagPickerDialog,
            navigateToSinglePicker = navigation::toSinglePickerDialog,
            navigateToAddOrEditNote = toAddOrEditNote,
            navigateToVideoPlayerScreen = navigation::toVideoPlayerScreen,
            navigateToImageViewerScreen = navigation::toImageViewerScreen,
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

        dialogFixedMaxHeight(
            route = TabletRightPaneDestinations.TAG_FILTER_DIALOG,
        ) {
            FilterScreen(onBack = navigation::onBack)
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
                onCloseClicked = navigation::onBack,
            )
        }
    }
}

private object TabletRightPaneDestinations {
    const val ALL_ITEMS_SORT_DIALOG = "allItemsSortDialog"
    const val CREATOR_EDIT_DIALOG = "creatorEditDialog"
    const val SINGLE_PICKER_DIALOG = "singlePickerDialog"
    const val TAG_PICKER_DIALOG = "tagPickerDialog"
    const val TAG_FILTER_DIALOG = "tagFilterDialog"
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

private fun ZoteroNavigation.toTagFilter() {
    navController.navigate(TabletRightPaneDestinations.TAG_FILTER_DIALOG)
}
