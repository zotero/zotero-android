
package org.zotero.android.screens.dashboard

import android.net.Uri
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.composable
import org.zotero.android.architecture.EventBusConstants.FileWasSelected.CallPoint
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.screenOrDialogDynamicHeight
import org.zotero.android.architecture.ui.screenOrDialogFixedHeight
import org.zotero.android.screens.addnote.AddNoteScreen
import org.zotero.android.screens.allitems.AllItemsScreen
import org.zotero.android.screens.creatoredit.CreatorEditNavigation
import org.zotero.android.screens.filter.FilterScreen
import org.zotero.android.screens.itemdetails.ItemDetailsScreen
import org.zotero.android.screens.loading.LoadingScreen
import org.zotero.android.screens.mediaviewer.image.ImageViewerScreen
import org.zotero.android.screens.mediaviewer.video.VideoPlayerView
import org.zotero.android.screens.sortpicker.SortPickerNavigation
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.sync.Library
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import org.zotero.android.uicomponents.singlepicker.SinglePickerScreen
import java.io.File

/**
 * If it's a phone, then this navigation screen takes full screen,
 * if it's a tablet, then it only occupies the right portion of the screen.
 */
@Composable
internal fun FullScreenOrRightPaneNavigation(
    onPickFile: (callPoint: CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onShowPdf: (file: File, key: String, library: Library) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    navController: NavHostController,
    navigation: Navigation,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    ZoteroNavHost(
        navController = navController,
        startDestination = FullScreenDestinations.ALL_ITEMS,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        collectionAtRootGraph(
            navController = navController,
            onBack = navigation::onBack,
            layoutType = layoutType,
        )
        loadingScreen()
        allItemsScreen(
            onBack = navigation::onBack,
            onPickFile = { onPickFile(CallPoint.AllItems) },
            onOpenFile = onOpenFile,
            onOpenWebpage = onOpenWebpage,
            navigateToCollectionsScreen = navigation::toCollectionsScreen,
            navigateToItemDetails = navigation::toItemDetails,
            navigateToAddOrEditNote = navigation::toAddOrEditNote,
            navigateToSinglePicker = navigation::toSinglePicker,
            navigateToAllItemsSort = navigation::toAllItemsSort,
            navigateToVideoPlayerScreen = navigation::toVideoPlayerScreen,
            navigateToImageViewerScreen = navigation::toImageViewerScreen,
            navigateToTagFilter = navigation::toTagFilter,
            onShowPdf = onShowPdf,
        )
        itemDetailsScreen(
            navigateToCreatorEdit = navigation::toCreatorEdit,
            navigateToTagPicker = navigation::toTagPicker,
            navigateToSinglePicker = navigation::toSinglePicker,
            navigateToAddOrEditNote = navigation::toAddOrEditNote,
            navigateToVideoPlayerScreen = navigation::toVideoPlayerScreen,
            navigateToImageViewerScreen = navigation::toImageViewerScreen,
            onBack = navigation::onBack,
            onOpenFile = onOpenFile,
            onOpenWebpage = onOpenWebpage,
            onPickFile = { onPickFile(CallPoint.ItemDetails) },
            onShowPdf = onShowPdf,
        )
        addNoteScreen(
            onBack = navigation::onBack
        )
        videoPlayerScreen()
        imageViewerScreen(onBack = navigation::onBack)
        screenOrDialogDynamicHeight(
            route = FullScreenDestinations.TAG_PICKER,
            layoutType = layoutType,
        ) {
            TagPickerScreen(onBack = navigation::onBack)
        }

        screenOrDialogFixedHeight(
            route = FullScreenDestinations.TAG_FILTER,
            layoutType = layoutType,
        ) {
            FilterScreen(onBack = navigation::onBack)
        }
        screenOrDialogFixedHeight(
            route = FullScreenDestinations.ALL_ITEMS_SORT,
            layoutType = layoutType,
        ) {
            SortPickerNavigation()
        }
        screenOrDialogFixedHeight(
            route = FullScreenDestinations.CREATOR_EDIT,
            layoutType = layoutType,
        ) {
            CreatorEditNavigation()
        }
        screenOrDialogDynamicHeight(
            route = FullScreenDestinations.SINGLE_PICKER,
            layoutType = layoutType,
        ) {
            SinglePickerScreen(
                onCloseClicked = navigation::onBack,
            )
        }
    }
}

private fun NavGraphBuilder.allItemsScreen(
    onBack: () -> Unit,
    navigateToCollectionsScreen: () -> Unit,
    navigateToItemDetails: () -> Unit,
    navigateToAddOrEditNote: () -> Unit,
    navigateToSinglePicker: () -> Unit,
    navigateToAllItemsSort: () -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    navigateToTagFilter: () -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    onPickFile: () -> Unit,
    onShowPdf: (file: File, key: String, library: Library) -> Unit,
) {
    composable(
        route = FullScreenDestinations.ALL_ITEMS,
        enterTransition = { EnterTransition.None },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) }) {
        AllItemsScreen(
            onBack = onBack,
            onPickFile = onPickFile,
            onOpenFile = onOpenFile,
            onOpenWebpage = onOpenWebpage,
            onShowPdf = onShowPdf,
            navigateToCollectionsScreen = navigateToCollectionsScreen,
            navigateToAddOrEditNote = navigateToAddOrEditNote,
            navigateToItemDetails = navigateToItemDetails,
            navigateToSinglePicker = navigateToSinglePicker,
            navigateToAllItemsSort = navigateToAllItemsSort,
            navigateToVideoPlayerScreen = navigateToVideoPlayerScreen,
            navigateToImageViewerScreen = navigateToImageViewerScreen,
            navigateToTagFilter = navigateToTagFilter,
        )
    }
}

private fun NavGraphBuilder.itemDetailsScreen(
    onBack: () -> Unit,
    navigateToCreatorEdit: () -> Unit,
    navigateToTagPicker: () -> Unit,
    navigateToSinglePicker: () -> Unit,
    navigateToAddOrEditNote: () -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    onPickFile: () -> Unit,
    onShowPdf: (file: File, key: String, library: Library) -> Unit,
) {
    composable(
        route = FullScreenDestinations.ITEM_DETAILS,
        arguments = listOf(),
    ) {
        ItemDetailsScreen(
            onBack = onBack,
            navigateToAddOrEditNote = navigateToAddOrEditNote,
            navigateToCreatorEdit = navigateToCreatorEdit,
            navigateToSinglePicker = navigateToSinglePicker,
            navigateToTagPicker = navigateToTagPicker,
            navigateToVideoPlayerScreen = navigateToVideoPlayerScreen,
            navigateToImageViewerScreen = navigateToImageViewerScreen,
            onOpenFile = onOpenFile,
            onOpenWebpage = onOpenWebpage,
            onPickFile = onPickFile,
            onShowPdf = onShowPdf
        )
    }
}

private fun NavGraphBuilder.addNoteScreen(
    onBack: () -> Unit,
) {
    composable(
        route = FullScreenDestinations.ADD_NOTE,
        arguments = listOf(),
    ) {
        AddNoteScreen(
            onBack = onBack,
        )
    }
}

private fun NavGraphBuilder.loadingScreen(
) {
    composable(
        route = FullScreenDestinations.LOADING,
        arguments = listOf(),
    ) {
        LoadingScreen()
    }
}

private fun NavGraphBuilder.videoPlayerScreen(
) {
    composable(
        route = FullScreenDestinations.VIDEO_PLAYER_SCREEN,
        arguments = listOf(),
    ) {
        VideoPlayerView()
    }
}

private fun NavGraphBuilder.imageViewerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = FullScreenDestinations.IMAGE_VIEWER_SCREEN,
        arguments = listOf(),
    ) {
        ImageViewerScreen(onBack = onBack)
    }
}

object FullScreenDestinations {
    const val LOADING = "loading"
    const val COLLECTIONS_SCREEN = "collectionsScreen"
    const val ALL_ITEMS = "allItems"
    const val ITEM_DETAILS = "itemDetails"
    const val ADD_NOTE = "addNote"
    const val VIDEO_PLAYER_SCREEN = "videoPlayerScreen"
    const val IMAGE_VIEWER_SCREEN = "imageViewerScreen"
    const val ALL_ITEMS_SORT = "allItemsSort"
    const val CREATOR_EDIT = "creatorEdit"
    const val SINGLE_PICKER = "singlePicker"
    const val TAG_PICKER = "tagPicker"
    const val TAG_FILTER = "tagFilter"
}

@SuppressWarnings("UseDataClass")
class Navigation(
    private val navController: NavHostController,
    private val onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    fun onBack() = onBackPressedDispatcher?.onBackPressed()



    fun toCollectionsScreen() {
        navController.navigate(FullScreenDestinations.COLLECTIONS_SCREEN) {
            launchSingleTop = true
        }
    }

    fun toItemDetails() {
        navController.navigate(FullScreenDestinations.ITEM_DETAILS)
    }

    fun toAddOrEditNote() {
        navController.navigate(FullScreenDestinations.ADD_NOTE)
    }

    fun toAllItemsSort() {
        navController.navigate(FullScreenDestinations.ALL_ITEMS_SORT)
    }

    fun toCreatorEdit() {
        navController.navigate(FullScreenDestinations.CREATOR_EDIT)
    }

    fun toTagPicker() {
        navController.navigate(FullScreenDestinations.TAG_PICKER)
    }

    fun toSinglePicker() {
        navController.navigate(FullScreenDestinations.SINGLE_PICKER)
    }

    fun toVideoPlayerScreen() {
        navController.navigate(FullScreenDestinations.VIDEO_PLAYER_SCREEN)
    }

    fun toImageViewerScreen() {
        navController.navigate(FullScreenDestinations.IMAGE_VIEWER_SCREEN)
    }

    fun toTagFilter() {
        navController.navigate(FullScreenDestinations.TAG_FILTER)
    }
}
