package org.zotero.android.architecture.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.slideInHorizontally
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import org.zotero.android.screens.addnote.AddNoteScreen
import org.zotero.android.screens.allitems.AllItemsScreen
import org.zotero.android.screens.collections.CollectionsScreen
import org.zotero.android.screens.itemdetails.ItemDetailsScreen
import org.zotero.android.screens.libraries.LibrariesScreen
import org.zotero.android.screens.loading.LoadingScreen
import org.zotero.android.screens.mediaviewer.image.ImageViewerScreen
import org.zotero.android.screens.mediaviewer.video.VideoPlayerView
import java.io.File

fun NavGraphBuilder.allItemsScreen(
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
    onShowPdf: () -> Unit,
) {
    composable(
        route = CommonScreenDestinations.ALL_ITEMS,
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

fun NavGraphBuilder.itemDetailsScreen(
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
    onShowPdf: () -> Unit,
) {
    composable(
        route = CommonScreenDestinations.ITEM_DETAILS,
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

fun NavGraphBuilder.addNoteScreen(
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
) {
    composable(
        route = CommonScreenDestinations.ADD_NOTE,
        arguments = listOf(),
    ) {
        AddNoteScreen(
            onBack = onBack,
            navigateToTagPicker = navigateToTagPicker,
        )
    }
}

fun NavGraphBuilder.librariesScreen(
    navigateToCollectionsScreen: () -> Unit,
    onSettingsTapped: () -> Unit,
) {
    composable(route = CommonScreenDestinations.LIBRARIES_SCREEN) {
        LibrariesScreen(
            onSettingsTapped = onSettingsTapped,
            navigateToCollectionsScreen = navigateToCollectionsScreen,
        )
    }
}

fun NavGraphBuilder.videoPlayerScreen() {
    composable(
        route = CommonScreenDestinations.VIDEO_PLAYER_SCREEN,
        arguments = listOf(),
    ) {
        VideoPlayerView()
    }
}

fun NavGraphBuilder.imageViewerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = CommonScreenDestinations.IMAGE_VIEWER_SCREEN,
        arguments = listOf(),
    ) {
        ImageViewerScreen(onBack = onBack)
    }
}

fun NavGraphBuilder.loadingScreen(
) {
    composable(
        route = CommonScreenDestinations.LOADING,
        arguments = listOf(),
    ) {
        LoadingScreen()
    }
}

fun NavGraphBuilder.collectionsScreen(
    onBack: () -> Unit,
    navigateToAllItems: () -> Unit,
    navigateToCollectionEdit: () -> Unit,
    navigateToLibraries: () -> Unit,
) {
    composable(route = CommonScreenDestinations.COLLECTIONS_SCREEN) {
        CollectionsScreen(
            onBack = onBack,
            navigateToAllItems = navigateToAllItems,
            navigateToLibraries = navigateToLibraries,
            navigateToCollectionEdit = navigateToCollectionEdit,
        )
    }
}

object CommonScreenDestinations {
    const val LOADING = "loading"
    const val LIBRARIES_SCREEN = "librariesScreen"

    const val ALL_ITEMS = "allItems"
    const val ITEM_DETAILS = "itemDetails"
    const val ADD_NOTE = "addNote"
    const val VIDEO_PLAYER_SCREEN = "videoPlayerScreen"
    const val IMAGE_VIEWER_SCREEN = "imageViewerScreen"
    const val COLLECTIONS_SCREEN = "collectionsScreen"
}


fun ZoteroNavigation.toItemDetails() {
    navController.navigate(CommonScreenDestinations.ITEM_DETAILS)
}

fun ZoteroNavigation.toAddOrEditNote() {
    navController.navigate(CommonScreenDestinations.ADD_NOTE)
}

fun ZoteroNavigation.toVideoPlayerScreen() {
    navController.navigate(CommonScreenDestinations.VIDEO_PLAYER_SCREEN)
}

fun ZoteroNavigation.toImageViewerScreen() {
    navController.navigate(CommonScreenDestinations.IMAGE_VIEWER_SCREEN)
}