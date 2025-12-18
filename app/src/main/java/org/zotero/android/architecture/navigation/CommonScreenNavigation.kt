package org.zotero.android.architecture.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.screens.addnote.AddNoteScreen
import org.zotero.android.screens.allitems.AllItemsScreen
import org.zotero.android.screens.collections.CollectionsScreen
import org.zotero.android.screens.itemdetails.ItemDetailsScreen
import org.zotero.android.screens.libraries.LibrariesScreen
import org.zotero.android.screens.loading.LoadingScreen
import org.zotero.android.screens.mediaviewer.image.ImageViewerScreen
import org.zotero.android.screens.mediaviewer.video.VideoPlayerView
import org.zotero.android.screens.webview.ZoteroWebViewScreen
import java.io.File

internal const val ARG_COLLECTIONS_SCREEN = "collectionArgs"
internal const val ARG_ITEM_DETAILS_SCREEN = "itemDetailsArgs"
internal const val ARG_RETRIEVE_METADATA = "retrieveMetadataArgs"
internal const val ARG_TAGS_FILTER = "tagsScreen"
internal const val ARG_ADD_OR_EDIT_NOTE = "notesScreen"

fun NavGraphBuilder.allItemsScreen(
    navigateToCollectionsScreen: (String) -> Unit,
    navigateToItemDetails: (String) -> Unit,
    navigateToAddOrEditNote: (String) -> Unit,
    navigateToSinglePicker: () -> Unit,
    navigateToAllItemsSort: () -> Unit,
    navigateToAddByIdentifier: (addByIdentifierParams: String) -> Unit,
    navigateToRetrieveMetadata: (params: String) -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    navigateToZoterWebViewScreen: (String) -> Unit,
    navigateToTagFilter: (params: String) -> Unit,
    navigateToCollectionPicker: () -> Unit,
    navigateToScanBarcode: () -> Unit,
    navigateToSingleCitation: () -> Unit,
    navigateToCitationBibliographyExport:() -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (url: String) -> Unit,
    onPickFile: () -> Unit,
    onShowPdf: (String) -> Unit,
    onShowHtmlOrEpub: (String) -> Unit,
    isTablet: Boolean
) {
    composable(
        route = CommonScreenDestinations.ALL_ITEMS,
        popEnterTransition = {
            if (isTablet) {
                EnterTransition.None
            } else {
                if (ScreenArguments.allItemsCollectionsLibsNavDirectionLeftToRight) {
                    slideInHorizontally(initialOffsetX = { -it })
                } else {
                    slideInHorizontally(initialOffsetX = { it })
                }
            }
        },
//        popExitTransition = {
//            if (ScreenArguments.allItemsCollectionsLibsNavDirectionLeftToRight) {
//                slideOutHorizontally(targetOffsetX = { it })
//            } else {
//                slideOutHorizontally(targetOffsetX = { it })
//            }
//        },
        exitTransition = {
            if (isTablet) {
                ExitTransition.None
            } else {
                if (ScreenArguments.allItemsCollectionsLibsNavDirectionLeftToRight) {
                    slideOutHorizontally(targetOffsetX = { -it })
                } else {
                    slideOutHorizontally(targetOffsetX = { it })
                }
            }

        },
        enterTransition = {
            if (isTablet) {
                EnterTransition.None
            } else {
                if (ScreenArguments.allItemsCollectionsLibsNavDirectionLeftToRight) {
                    slideInHorizontally(initialOffsetX = { it })
                } else {
                    slideInHorizontally(initialOffsetX = { -it })
                }
            }
        }) {
        AllItemsScreen(
            onPickFile = onPickFile,
            onOpenFile = onOpenFile,
            onOpenWebpage = onOpenWebpage,
            onShowPdf = onShowPdf,
            onShowHtmlOrEpub =  onShowHtmlOrEpub,
            navigateToCollectionsScreen = navigateToCollectionsScreen,
            navigateToAddOrEditNote = navigateToAddOrEditNote,
            navigateToItemDetails = navigateToItemDetails,
            navigateToSinglePicker = navigateToSinglePicker,
            navigateToAllItemsSort = navigateToAllItemsSort,
            navigateToAddByIdentifier = navigateToAddByIdentifier,
            navigateToZoterWebViewScreen = navigateToZoterWebViewScreen,
            navigateToVideoPlayerScreen = navigateToVideoPlayerScreen,
            navigateToImageViewerScreen = navigateToImageViewerScreen,
            navigateToTagFilter = navigateToTagFilter,
            navigateToCollectionPicker = navigateToCollectionPicker,
            navigateToScanBarcode = navigateToScanBarcode,
            navigateToRetrieveMetadata = navigateToRetrieveMetadata,
            navigateToSingleCitation = navigateToSingleCitation,
            navigateToCitationBibliographyExport = navigateToCitationBibliographyExport
        )
    }
}

fun NavGraphBuilder.itemDetailsScreen(
    onBack: () -> Unit,
    navigateToCreatorEdit: () -> Unit,
    navigateToTagPicker: () -> Unit,
    navigateToSinglePicker: () -> Unit,
    navigateToAddOrEditNote: (String) -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    navigateToZoterWebViewScreen: (String) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (url: String) -> Unit,
    onPickFile: () -> Unit,
    onShowPdf: (String) -> Unit,
    onShowHtmlOrEpub: (String) -> Unit,
) {

    composable(
        route = "${CommonScreenDestinations.ITEM_DETAILS}/{$ARG_ITEM_DETAILS_SCREEN}",
        arguments = listOf(
            navArgument(ARG_ITEM_DETAILS_SCREEN) { type = NavType.StringType },
        ),
    ) {
        ItemDetailsScreen(
            onBack = onBack,
            navigateToAddOrEditNote = navigateToAddOrEditNote,
            navigateToCreatorEdit = navigateToCreatorEdit,
            navigateToSinglePicker = navigateToSinglePicker,
            navigateToTagPicker = navigateToTagPicker,
            navigateToZoterWebViewScreen = navigateToZoterWebViewScreen,
            navigateToVideoPlayerScreen = navigateToVideoPlayerScreen,
            navigateToImageViewerScreen = navigateToImageViewerScreen,
            onOpenFile = onOpenFile,
            onOpenWebpage = onOpenWebpage,
            onPickFile = onPickFile,
            onShowPdf = onShowPdf,
            onShowHtmlOrEpub = onShowHtmlOrEpub,
        )
    }
}

fun NavGraphBuilder.addNoteScreen(
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
) {
    composable(
        route = "${CommonScreenDestinations.ADD_NOTE}/{$ARG_ADD_OR_EDIT_NOTE}",
        arguments = listOf(
            navArgument(ARG_ADD_OR_EDIT_NOTE) { type = NavType.StringType },
        ),
    ) {
        AddNoteScreen(
            onBack = onBack,
            navigateToTagPicker = navigateToTagPicker,
        )
    }
}

private const val ARG_WEBVIEW_URL = "ARG_WEBVIEW_URL"

fun NavGraphBuilder.zoterWebViewScreen(onClose: () -> Unit) {
    composable(
        route = "${CommonScreenDestinations.ZOTERO_WEB_VIEW_SCREEN}/{$ARG_WEBVIEW_URL}",
        arguments = listOf(
            navArgument(ARG_WEBVIEW_URL) { type = NavType.StringType },
        ),
    ) { backStackEntry ->
        val url = backStackEntry.arguments?.getString(ARG_WEBVIEW_URL) ?: return@composable
        ZoteroWebViewScreen(
            url = url,
            onClose = onClose,
        )
    }
}

fun NavGraphBuilder.librariesScreen(
    navigateToCollectionsScreen: (String) -> Unit,
    onSettingsTapped: () -> Unit,
    onExitApp:() -> Unit,
) {
    composable(
        route = CommonScreenDestinations.LIBRARIES_SCREEN,
        enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
//        popExitTransition = { slideOutHorizontally(targetOffsetX = { -it }) }
    )
    {
        LibrariesScreen(
            onSettingsTapped = onSettingsTapped,
            navigateToCollectionsScreen = navigateToCollectionsScreen,
            onExitApp = onExitApp,
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
    collectionDefaultValue: String,
    onBack: () -> Unit,
    navigateToAllItems: (String) -> Unit,
    navigateToCollectionEdit: () -> Unit,
    navigateToLibraries: (String) -> Unit,
    isTablet: Boolean,
) {
    composable(
        route = "${CommonScreenDestinations.COLLECTIONS_SCREEN}/{$ARG_COLLECTIONS_SCREEN}",
        arguments = listOf(
            navArgument(ARG_COLLECTIONS_SCREEN) {
                type = NavType.StringType; defaultValue = collectionDefaultValue
            },
        ),
//        popExitTransition = {
//            if (ScreenArguments.allItemsCollectionsLibsNavDirectionLeftToRight) {
//                slideOutHorizontally(targetOffsetX = { it })
//            } else {
//                slideOutHorizontally(targetOffsetX = { -it })
//            }
//        },
        popEnterTransition = {
            if (ScreenArguments.allItemsCollectionsLibsNavDirectionLeftToRight) {
                slideInHorizontally(initialOffsetX = { -it })
            } else {
                slideInHorizontally(initialOffsetX = { it })
            }
        },
        enterTransition = {
            if (ScreenArguments.allItemsCollectionsLibsNavDirectionLeftToRight) {
                slideInHorizontally(initialOffsetX = { it })
            } else {
                slideInHorizontally(initialOffsetX = { -it })
            }
        },
        exitTransition = {
            if (!isTablet && ScreenArguments.allItemsCollectionsLibsNavDirectionLeftToRight) {
                slideOutHorizontally(targetOffsetX = { -it })
            } else {
                slideOutHorizontally(targetOffsetX = { it })
            }

        },
    ) {
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
    const val ZOTERO_WEB_VIEW_SCREEN = "zoteroWebViewScreen"
}


fun ZoteroNavigation.toItemDetails(args: String) {
    navController.navigate("${CommonScreenDestinations.ITEM_DETAILS}/$args")
}

fun ZoteroNavigation.toAddOrEditNote(args: String) {
    navController.navigate("${CommonScreenDestinations.ADD_NOTE}/$args")
}

fun ZoteroNavigation.toVideoPlayerScreen() {
    navController.navigate(CommonScreenDestinations.VIDEO_PLAYER_SCREEN)
}

fun ZoteroNavigation.toImageViewerScreen() {
    navController.navigate(CommonScreenDestinations.IMAGE_VIEWER_SCREEN)
}

fun ZoteroNavigation.toZoteroWebViewScreen(encodedUrl: String) {
    navController.navigate(
        "${CommonScreenDestinations.ZOTERO_WEB_VIEW_SCREEN}/$encodedUrl"
    )
}
