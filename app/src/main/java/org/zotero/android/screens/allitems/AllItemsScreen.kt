
package org.zotero.android.screens.allitems

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.error.FullScreenError
import org.zotero.android.uicomponents.loading.BaseLceBox
import org.zotero.android.uicomponents.loading.CircularLoading
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import java.io.File

@Composable
internal fun AllItemsScreen(
    viewModel: AllItemsViewModel = hiltViewModel(),
    onPickFile: () -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    navigateToCollectionsScreen: () -> Unit,
    navigateToSinglePicker: () -> Unit,
    navigateToAllItemsSort: () -> Unit,
    navigateToItemDetails: () -> Unit,
    navigateToAddOrEditNote: () -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    navigateToZoterWebViewScreen: (String) -> Unit,
    navigateToTagFilter: () -> Unit,
    onShowPdf: () -> Unit,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(AllItemsViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
            is AllItemsViewEffect.ShowCollectionsEffect -> navigateToCollectionsScreen()
            is AllItemsViewEffect.ShowItemDetailEffect -> navigateToItemDetails()
            is AllItemsViewEffect.ShowAddOrEditNoteEffect -> navigateToAddOrEditNote()
            AllItemsViewEffect.ShowFilterEffect -> {
                navigateToTagFilter()
            }
            AllItemsViewEffect.ShowItemTypePickerEffect -> {
                navigateToSinglePicker()
            }
            AllItemsViewEffect.ShowSortPickerEffect -> {
                navigateToAllItemsSort()
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
                onShowPdf()
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
        SwipeRefresh(
            state = rememberSwipeRefreshState(viewState.isRefreshing),
            onRefresh = viewModel::startSync,
            indicator = { state, trigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = trigger,
                    scale = true,
                    contentColor = CustomTheme.colors.dynamicTheme.primaryColor,
                )
            }
        ) {
            BaseLceBox(
                modifier = Modifier.fillMaxSize(),
                lce = viewState.lce,
                error = { _ ->
                    FullScreenError(
                        modifier = Modifier.align(Alignment.Center),
                        errorTitle = stringResource(id = Strings.all_items_load_error),
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
                    AllItemsSearchBar(
                        viewState = viewState,
                        viewModel = viewModel
                    )
                    CustomDivider()
                    AllItemsTable(viewState, layoutType, viewModel)
                }
            }

            val itemsError = viewState.error
            if (itemsError != null) {
                ShowErrorOrDialog(
                    itemsError = itemsError,
                    onDismissDialog = viewModel::onDismissDialog,
                    onDeleteItems = { viewModel.delete(it) },
                    onEmptyTrash = { viewModel.emptyTrash() }
                )
            }

            AllItemsAddBottomSheet(
                onAddFile = onPickFile,
                onAddNote = viewModel::onAddNote,
                onAddManually = viewModel::onAddManually,
                onClose = viewModel::onAddBottomSheetCollapse,
                showBottomSheet = viewState.shouldShowAddBottomSheet
            )
        }

    }
}