package org.zotero.android.architecture.navigation.phone

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.navigation.CommonScreenDestinations
import org.zotero.android.architecture.navigation.DashboardTopLevelDialogs
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
import org.zotero.android.architecture.navigation.videoPlayerScreen
import org.zotero.android.pdf.pdfReaderNavScreens
import org.zotero.android.pdf.toPdfScreen
import org.zotero.android.screens.collectionedit.collectionEditNavScreens
import org.zotero.android.screens.collectionedit.toCollectionEditScreen
import org.zotero.android.screens.creatoredit.creatorEditNavScreens
import org.zotero.android.screens.creatoredit.toCreatorEdit
import org.zotero.android.screens.dashboard.BuildInfo
import org.zotero.android.screens.dashboard.DashboardViewModel
import org.zotero.android.screens.dashboard.DashboardViewState
import org.zotero.android.screens.filter.FilterScreen
import org.zotero.android.screens.settings.settingsNavScreens
import org.zotero.android.screens.settings.toSettingsScreen
import org.zotero.android.screens.sortpicker.sortPickerNavScreens
import org.zotero.android.screens.sortpicker.toSortPicker
import org.zotero.android.screens.tagpicker.TagPickerScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost
import org.zotero.android.uicomponents.singlepicker.SinglePickerScreen
import org.zotero.android.uicomponents.systemui.SolidStatusBar
import org.zotero.android.uicomponents.theme.CustomTheme
import java.io.File

@Composable
internal fun DashboardRootPhoneNavigation(
    onPickFile: (callPoint: EventBusConstants.FileWasSelected.CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    viewModel: DashboardViewModel,
) {
    val viewState by viewModel.viewStates.observeAsState(DashboardViewState())
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    SolidStatusBar()

    val navController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    Box{
        Column(modifier = Modifier.background(color = CustomTheme.colors.surface)) {
            BuildInfo()
            ZoteroNavHost(
                navController = navController,
                startDestination = CommonScreenDestinations.ALL_ITEMS,
                modifier = Modifier.navigationBarsPadding(), // do not draw behind nav bar
            ) {
                collectionsScreen(
                    onBack = navigation::onBack,
                    navigateToAllItems = {
                        toAllItems(
                            navController = navController,
                        )
                    },
                    navigateToLibraries = {
                        navController.navigate(CommonScreenDestinations.LIBRARIES_SCREEN)
                    },
                    navigateToCollectionEdit = { navigation.toCollectionEditScreen() },
                )
                librariesScreen(
                    navigateToCollectionsScreen = {
                        navController.popBackStack(navController.graph.id, inclusive = true)
                        navController.navigate(CommonScreenDestinations.LIBRARIES_SCREEN)
                        navController.navigate(CommonScreenDestinations.COLLECTIONS_SCREEN)
                    },
                    onSettingsTapped = { navigation.toSettingsScreen() }
                )

                loadingScreen()
                allItemsScreen(
                    onBack = navigation::onBack,
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
                    navigateToTagFilter = navigation::toTagFilter,
                    onShowPdf = navigation::toPdfScreen,
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
                    onPickFile = { onPickFile(EventBusConstants.FileWasSelected.CallPoint.ItemDetails) },
                    onShowPdf = navigation::toPdfScreen,
                )

                composable(
                    route = DashboardRootPhoneDestinations.TAG_PICKER,
                    arguments = listOf(),
                ) {
                    TagPickerScreen(onBack = navigation::onBack)
                }

                composable(
                    route = DashboardRootPhoneDestinations.TAG_FILTER,
                    arguments = listOf(),
                ) {
                    FilterScreen(onBack = navigation::onBack)
                }

                composable(
                    route = DashboardRootPhoneDestinations.SINGLE_PICKER,
                    arguments = listOf(),
                ) {
                    SinglePickerScreen(
                        onCloseClicked = navigation::onBack,
                    )
                }

                sortPickerNavScreens(navigation)
                creatorEditNavScreens(navigation)
                collectionEditNavScreens(navigation)
                settingsNavScreens(navigation = navigation, onOpenWebpage = onOpenWebpage)

                videoPlayerScreen()
                imageViewerScreen(onBack = navigation::onBack)

                pdfReaderNavScreens(navigation)
                addNoteScreen(
                    onBack = navigation::onBack,
                    navigateToTagPicker = navigation::toTagPicker
                )
            }
        }
        DashboardTopLevelDialogs(viewState = viewState, viewModel = viewModel)
    }
}

private object DashboardRootPhoneDestinations {
    const val SINGLE_PICKER = "singlePicker"
    const val TAG_PICKER = "tagPicker"
    const val TAG_FILTER = "tagFilter"

}

private fun ZoteroNavigation.toCollectionsScreen() {
    navController.navigate(CommonScreenDestinations.COLLECTIONS_SCREEN) {
        launchSingleTop = true
    }
}

private fun ZoteroNavigation.toTagPicker() {
    navController.navigate(DashboardRootPhoneDestinations.TAG_PICKER)
}

private fun ZoteroNavigation.toSinglePicker() {
    navController.navigate(DashboardRootPhoneDestinations.SINGLE_PICKER)
}

private fun ZoteroNavigation.toTagFilter() {
    navController.navigate(DashboardRootPhoneDestinations.TAG_FILTER)
}

private fun toAllItems(
    navController: NavHostController,
) {
    navController.popBackStack(navController.graph.id, inclusive = true)
    navController.navigate(CommonScreenDestinations.COLLECTIONS_SCREEN)
    navController.navigate(CommonScreenDestinations.ALL_ITEMS)
}

