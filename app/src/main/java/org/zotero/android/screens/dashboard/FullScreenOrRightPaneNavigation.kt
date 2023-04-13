
package org.zotero.android.screens.dashboard

import android.net.Uri
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.composable
import org.zotero.android.architecture.EventBusConstants.FileWasSelected.CallPoint
import org.zotero.android.architecture.ui.CustomLayoutSize
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
    onShowPdf: (file: File) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    navController: NavHostController,
    navigation: Navigation,
) {
    val isTablet = CustomLayoutSize.calculateLayoutType().isTablet()
    ZoteroNavHost(
        navController = navController,
        startDestination = FullScreenDestinations.ALL_ITEMS,
        modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
    ) {
        collectionAsRootGraph(
            navController = navController,
            onBack = navigation::onBack,
            isTablet = isTablet,
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
            navigateToSinglePickerScreen = navigation::toSinglePickerScreen,
            navigateToSinglePickerDialog = navigation::toSinglePickerDialog,
            navigateToAllItemsSortScreen = navigation::toAllItemsSortScreen,
            navigateToAllItemsSortDialog = navigation::toAllItemsSortDialog,
            navigateToVideoPlayerScreen = navigation::toVideoPlayerScreen,
            navigateToImageViewerScreen = navigation::toImageViewerScreen,
            navigateToFilterScreen = navigation::toFilterScreen,
            navigateToFilterDialog = navigation::toFilterDialog,
            onShowPdf = onShowPdf,
        )
        itemDetailsScreen(
            navigateToCreatorEditScreen = navigation::toCreatorEditScreen,
            navigateToCreatorEditDialog = navigation::toCreatorEditDialog,
            navigateToTagPickerScreen = navigation::toTagPickerScreen,
            navigateToTagPickerDialog = navigation::toTagPickerDialog,
            navigateToSinglePickerScreen = navigation::toSinglePickerScreen,
            navigateToSinglePickerDialog = navigation::toSinglePickerDialog,
            navigateToAddOrEditNote = navigation::toAddOrEditNote,
            navigateToVideoPlayerScreen = navigation::toVideoPlayerScreen,
            navigateToImageViewerScreen = navigation::toImageViewerScreen,
            onBack = navigation::onBack,
            onOpenFile = onOpenFile,
            onOpenWebpage = onOpenWebpage,
            onPickFile = { onPickFile(CallPoint.ItemDetails) },
            onShowPdf = onShowPdf
        )
        addNoteScreen(
            onBack = navigation::onBack
        )
        allItemsSortScreen()
        allItemsSortDialog()
        creatorEditScreen()
        creatorEditDialog()
        singlePickerScreen(onBack = navigation::onBack)
        singlePickerDialog(onBack = navigation::onBack)
        videoPlayerScreen()
        imageViewerScreen(onBack = navigation::onBack)
        toTagPickerScreen(onBack = navigation::onBack)
        toTagPickerDialog(onBack = navigation::onBack)
        toFilterScreen(onBack = navigation::onBack)
        toFilterDialog(onBack = navigation::onBack)
    }
}

private fun NavGraphBuilder.allItemsScreen(
    onBack: () -> Unit,
    navigateToCollectionsScreen: () -> Unit,
    navigateToItemDetails: () -> Unit,
    navigateToAddOrEditNote: () -> Unit,
    navigateToSinglePickerScreen: () -> Unit,
    navigateToSinglePickerDialog: () -> Unit,
    navigateToAllItemsSortScreen: () -> Unit,
    navigateToAllItemsSortDialog: () -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    navigateToFilterScreen: () -> Unit,
    navigateToFilterDialog: () -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    onPickFile: () -> Unit,
    onShowPdf: (file: File) -> Unit,
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
            navigateToSinglePickerScreen = navigateToSinglePickerScreen,
            navigateToSinglePickerDialog = navigateToSinglePickerDialog,
            navigateToAllItemsSortScreen = navigateToAllItemsSortScreen,
            navigateToAllItemsSortDialog = navigateToAllItemsSortDialog,
            navigateToVideoPlayerScreen = navigateToVideoPlayerScreen,
            navigateToImageViewerScreen = navigateToImageViewerScreen,
            navigateToFilterScreen = navigateToFilterScreen,
            navigateToFilterDialog = navigateToFilterDialog,
        )
    }
}

private fun NavGraphBuilder.itemDetailsScreen(
    onBack: () -> Unit,
    navigateToCreatorEditScreen: () -> Unit,
    navigateToCreatorEditDialog: () -> Unit,
    navigateToTagPickerScreen: () -> Unit,
    navigateToTagPickerDialog: () -> Unit,
    navigateToSinglePickerScreen: () -> Unit,
    navigateToSinglePickerDialog: () -> Unit,
    navigateToAddOrEditNote: () -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    onPickFile: () -> Unit,
    onShowPdf: (file: File) -> Unit,
) {
    composable(
        route = FullScreenDestinations.ITEM_DETAILS,
        arguments = listOf(),
    ) {
        ItemDetailsScreen(
            onBack = onBack,
            navigateToAddOrEditNote = navigateToAddOrEditNote,
            navigateToCreatorEditScreen = navigateToCreatorEditScreen,
            navigateToCreatorEditDialog = navigateToCreatorEditDialog,
            navigateToSinglePickerScreen = navigateToSinglePickerScreen,
            navigateToSinglePickerDialog = navigateToSinglePickerDialog,
            navigateToTagPickerScreen = navigateToTagPickerScreen,
            navigateToTagPickerDialog = navigateToTagPickerDialog,
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

private fun NavGraphBuilder.allItemsSortScreen(
) {
    composable(
        route = FullScreenDestinations.ALL_ITEMS_SORT_SCREEN,
        arguments = listOf(),
    ) {
        SortPickerNavigation()
    }
}

private fun NavGraphBuilder.allItemsSortDialog(
) {
    dialog(
        route = FullScreenDestinations.ALL_ITEMS_SORT_DIALOG,
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
        ) {
            SortPickerNavigation(scaffoldModifier = Modifier.requiredHeightIn(max = 400.dp))
        }
    }
}

private fun NavGraphBuilder.creatorEditScreen(
) {
    composable(
        route = FullScreenDestinations.CREATOR_EDIT_SCREEN,
        arguments = listOf(),
    ) {
        CreatorEditNavigation()
    }
}

private fun NavGraphBuilder.creatorEditDialog(
) {
    dialog(
        route = FullScreenDestinations.CREATOR_EDIT_DIALOG,
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
        ) {
            CreatorEditNavigation(scaffoldModifier = Modifier.requiredHeightIn(max = 400.dp))
        }
    }
}

private fun NavGraphBuilder.singlePickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = FullScreenDestinations.SINGLE_PICKER_SCREEN,
        arguments = listOf(),
    ) {
        SinglePickerScreen(onCloseClicked = onBack)
    }
}

private fun NavGraphBuilder.singlePickerDialog(
    onBack: () -> Unit,
) {
    dialog(
        route = FullScreenDestinations.SINGLE_PICKER_DIALOG,
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
        ) {
            SinglePickerScreen(
                onCloseClicked = onBack,
                scaffoldModifier = Modifier.fillMaxHeight(0.8f)
            )
        }
    }
}

private fun NavGraphBuilder.toTagPickerScreen(
    onBack: () -> Unit,
) {
    composable(
        route = FullScreenDestinations.TAG_PICKER_SCREEN,
        arguments = listOf(),
    ) {
        TagPickerScreen(onBack = onBack)
    }
}

private fun NavGraphBuilder.toTagPickerDialog(
    onBack: () -> Unit,
) {
    dialog(
        route = FullScreenDestinations.TAG_PICKER_DIALOG,
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
        ) {
            TagPickerScreen(
                scaffoldModifier = Modifier.requiredHeightIn(max = 400.dp),
                onBack = onBack
            )
        }
    }
}

private fun NavGraphBuilder.toFilterScreen(
    onBack: () -> Unit,
) {
    composable(
        route = FullScreenDestinations.FILTER_SCREEN,
        arguments = listOf(),
    ) {
        FilterScreen(onBack = onBack)
    }
}

private fun NavGraphBuilder.toFilterDialog(
    onBack: () -> Unit,
) {
    dialog(
        route = FullScreenDestinations.FILTER_DIALOG,
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
        ) {
            FilterScreen(
                scaffoldModifier = Modifier.requiredHeightIn(max = 400.dp),
                onBack = onBack
            )
        }
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
    const val ALL_ITEMS_SORT_SCREEN = "allItemsSortScreen"
    const val ALL_ITEMS_SORT_DIALOG = "allItemsSortDialog"
    const val CREATOR_EDIT_SCREEN = "creatorEditScreen"
    const val CREATOR_EDIT_DIALOG = "creatorEditDialog"
    const val SINGLE_PICKER_SCREEN = "singlePickerScreen"
    const val SINGLE_PICKER_DIALOG = "singlePickerDialog"
    const val TAG_PICKER_SCREEN = "tagPickerScreen"
    const val TAG_PICKER_DIALOG = "tagPickerDialog"
    const val FILTER_SCREEN = "tagFilterScreen"
    const val FILTER_DIALOG = "tagFilterDialog"
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

    fun toAllItemsSortScreen() {
        navController.navigate(FullScreenDestinations.ALL_ITEMS_SORT_SCREEN)
    }

    fun toAllItemsSortDialog() {
        navController.navigate(FullScreenDestinations.ALL_ITEMS_SORT_DIALOG)
    }

    fun toCreatorEditScreen() {
        navController.navigate(FullScreenDestinations.CREATOR_EDIT_SCREEN)
    }

    fun toCreatorEditDialog() {
        navController.navigate(FullScreenDestinations.CREATOR_EDIT_DIALOG)
    }

    fun toTagPickerScreen() {
        navController.navigate(FullScreenDestinations.TAG_PICKER_SCREEN)
    }

    fun toTagPickerDialog() {
        navController.navigate(FullScreenDestinations.TAG_PICKER_DIALOG)
    }

    fun toSinglePickerScreen() {
        navController.navigate(FullScreenDestinations.SINGLE_PICKER_SCREEN)
    }

    fun toSinglePickerDialog() {
        navController.navigate(FullScreenDestinations.SINGLE_PICKER_DIALOG)
    }

    fun toVideoPlayerScreen() {
        navController.navigate(FullScreenDestinations.VIDEO_PLAYER_SCREEN)
    }

    fun toImageViewerScreen() {
        navController.navigate(FullScreenDestinations.IMAGE_VIEWER_SCREEN)
    }

    fun toFilterScreen() {
        navController.navigate(FullScreenDestinations.FILTER_SCREEN)
    }

    fun toFilterDialog() {
        navController.navigate(FullScreenDestinations.FILTER_DIALOG)
    }
}
