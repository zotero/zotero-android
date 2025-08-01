package org.zotero.android.screens.allitems

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.allitems.table.AllItemsTable
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.error.FullScreenError
import org.zotero.android.uicomponents.loading.BaseLceBox
import org.zotero.android.uicomponents.loading.CircularLoading
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars
import java.io.File

@Composable
internal fun AllItemsScreen(
    viewModel: AllItemsViewModel = hiltViewModel(),
    onPickFile: () -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    navigateToCollectionsScreen: (String) -> Unit,
    navigateToSinglePicker: () -> Unit,
    navigateToAddByIdentifier: (addByIdentifierParams: String) -> Unit,
    navigateToRetrieveMetadata: (params: String) -> Unit,
    navigateToAllItemsSort: () -> Unit,
    navigateToCollectionPicker: () -> Unit,
    navigateToItemDetails: (String) -> Unit,
    navigateToAddOrEditNote: (String) -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    navigateToZoterWebViewScreen: (String) -> Unit,
    navigateToTagFilter: (params: String) -> Unit,
    navigateToScanBarcode: () -> Unit,
    navigateToSingleCitation: () -> Unit,
    onShowPdf: (String) -> Unit,
) {
    CustomThemeWithStatusAndNavBars(statusBarBackgroundColor = CustomTheme.colors.topBarBackgroundColor) {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val viewState by viewModel.viewStates.observeAsState(AllItemsViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        val lazyListState = rememberLazyListState()

        val isTablet = layoutType.isTablet()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init(isTablet)
        }

        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                null -> Unit
                is AllItemsViewEffect.ShowCollectionsEffect -> navigateToCollectionsScreen(consumedEffect.screenArgs)
                is AllItemsViewEffect.ShowItemDetailEffect -> navigateToItemDetails(consumedEffect.screenArgs)
                is AllItemsViewEffect.ShowAddOrEditNoteEffect -> navigateToAddOrEditNote(consumedEffect.screenArgs)
                is AllItemsViewEffect.ShowPhoneFilterEffect -> {
                    navigateToTagFilter(consumedEffect.params)
                }

                AllItemsViewEffect.ShowItemTypePickerEffect -> {
                    navigateToSinglePicker()
                }

                is AllItemsViewEffect.ShowAddByIdentifierEffect -> {
                    navigateToAddByIdentifier(consumedEffect.params)
                }

                is AllItemsViewEffect.ShowRetrieveMetadataDialogEffect -> {
                    navigateToRetrieveMetadata(consumedEffect.params)
                }

                AllItemsViewEffect.ShowSortPickerEffect -> {
                    navigateToAllItemsSort()
                }

                AllItemsViewEffect.ShowCollectionPickerEffect -> {
                    navigateToCollectionPicker()
                }

                AllItemsViewEffect.ScreenRefresh -> {
                    //no-op
                }

                is AllItemsViewEffect.OpenFile -> onOpenFile(
                    consumedEffect.file,
                    consumedEffect.mimeType
                )

                is AllItemsViewEffect.OpenWebpage -> onOpenWebpage(consumedEffect.uri)
                is AllItemsViewEffect.NavigateToPdfScreen -> {
                    onShowPdf(consumedEffect.params)
                }

                is AllItemsViewEffect.ShowVideoPlayer -> {
                    navigateToVideoPlayerScreen()
                }

                is AllItemsViewEffect.ShowImageViewer -> {
                    navigateToImageViewerScreen()
                }

                is AllItemsViewEffect.ShowZoteroWebView -> {
                    navigateToZoterWebViewScreen(consumedEffect.url)
                }

                is AllItemsViewEffect.ShowScanBarcode -> {
                    navigateToScanBarcode()
                }

                is AllItemsViewEffect.MaybeScrollToTop -> {
                    val maybeIndex =
                        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                    //Indices of the first visible item AFTER an update, so after the potential new item was added to the top.
                    if (consumedEffect.shouldScrollToTop && ((1..2).contains(maybeIndex))) {
                        lazyListState.scrollToItem(index = 0, scrollOffset = 0)
                    }
                }
                AllItemsViewEffect.ShowSingleCitationEffect -> {
                    navigateToSingleCitation()
                }
            }
        }

        CustomScaffold(
            topBar = {
                AllItemsTopBar(
                    viewState = viewState,
                    viewModel = viewModel,
                    layoutType = layoutType,
                )
            },
        ) {
            val refreshing = viewState.isRefreshing
            val pullRefreshState = rememberPullRefreshState(refreshing, { viewModel.startSync() })

            BaseLceBox(
                modifier = Modifier.fillMaxSize(),
                lce = viewState.lce,
                error = { _ ->
                    FullScreenError(
                        modifier = Modifier.align(Alignment.Center),
                        errorTitle = stringResource(id = Strings.error_list_load_check_crash_logs),
                    )
                },
                loading = {
                    CircularLoading()
                },
            ) {
                AllItemsBottomPanel(layoutType, viewState, viewModel)
                Column(
                    modifier = Modifier
                        .padding(bottom = layoutType.calculateAllItemsBottomPanelHeight())
                ) {
                    if (!layoutType.isTablet()) {
                        Column(modifier = Modifier.background(CustomTheme.colors.topBarBackgroundColor)) {
                            AllItemsSearchBar(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                viewState = viewState,
                                viewModel = viewModel
                            )
                            Spacer(
                                modifier = Modifier
                                    .height(16.dp)
                            )
                            NewDivider()
                        }
                    }
                    AllItemsTable(
                        lazyListState = lazyListState,
                        layoutType = layoutType,
                        itemCellModels = viewState.itemCellModels,
                        isEditing = viewState.isEditing,
                        pullRefreshState = pullRefreshState,
                        isItemSelected = viewState::isSelected,
                        getItemAccessory = viewState::getAccessoryForItem,
                        onItemTapped = viewModel::onItemTapped,
                        onAccessoryTapped = viewModel::onAccessoryTapped,
                        onItemLongTapped = viewModel::onItemLongTapped
                    )
                }

                val itemsError = viewState.error
                if (itemsError != null) {
                    ShowErrorOrDialog(
                        itemsError = itemsError,
                        onDismissDialog = viewModel::onDismissDialog,
                        onDeleteItems = { viewModel.delete(it) },
                        onEmptyTrash = { viewModel.emptyTrash() },
                        deleteItemsFromCollection = { viewModel.deleteItemsFromCollection(it) },
                    )
                }
                val bottomSheetTitle = stringResource(id = Strings.item_type)
                AllItemsAddBottomSheet(
                    onScanBarcode = viewModel::onScanBarcode,
                    onAddFile = onPickFile,
                    onAddNote = viewModel::onAddNote,
                    onAddManually = { viewModel.onAddManually(bottomSheetTitle) },
                    onAddByIdentifier = viewModel::onAddByIdentifier,
                    onClose = viewModel::onAddBottomSheetCollapse,
                    showBottomSheet = viewState.shouldShowAddBottomSheet
                )

                PullRefreshIndicator(
                    refreshing = refreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    contentColor = CustomTheme.colors.dynamicTheme.primaryColor,
                )
            }
        }

    }
}