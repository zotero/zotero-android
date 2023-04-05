
package org.zotero.android.screens.allitems

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.bottomsheet.LongPressBottomSheet
import org.zotero.android.uicomponents.error.FullScreenError
import org.zotero.android.uicomponents.loading.BaseLceBox
import org.zotero.android.uicomponents.loading.CircularLoading
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.textinput.SearchBar
import java.io.File

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun AllItemsScreen(
    onBack: () -> Unit,
    viewModel: AllItemsViewModel = hiltViewModel(),
    onPickFile: () -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    navigateToCollectionsScreen: () -> Unit,
    navigateToSinglePickerScreen: () -> Unit,
    navigateToSinglePickerDialog: () -> Unit,
    navigateToAllItemsSortScreen: () -> Unit,
    navigateToAllItemsSortDialog: () -> Unit,
    navigateToItemDetails: () -> Unit,
    navigateToAddOrEditNote: () -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    navigateToFilterScreen: () -> Unit,
    navigateToFilterDialog: () -> Unit,
    onShowPdf: (file: File) -> Unit,
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
                when (layoutType.showScreenOrDialog()) {
                    CustomLayoutSize.ScreenOrDialogToShow.SCREEN -> {
                        navigateToFilterScreen()
                    }
                    CustomLayoutSize.ScreenOrDialogToShow.DIALOG -> {
                        navigateToFilterDialog()
                    }
                }
            }
            AllItemsViewEffect.ShowItemTypePickerEffect -> {
                when (layoutType.showScreenOrDialog()) {
                    CustomLayoutSize.ScreenOrDialogToShow.SCREEN -> {
                        navigateToSinglePickerScreen()
                    }
                    CustomLayoutSize.ScreenOrDialogToShow.DIALOG -> {
                        navigateToSinglePickerDialog()
                    }
                }
            }
            AllItemsViewEffect.ShowSortPickerEffect -> {
                when (layoutType.showScreenOrDialog()) {
                    CustomLayoutSize.ScreenOrDialogToShow.SCREEN -> {
                        navigateToAllItemsSortScreen()
                    }
                    CustomLayoutSize.ScreenOrDialogToShow.DIALOG -> {
                        navigateToAllItemsSortDialog()
                    }
                }
            }
            AllItemsViewEffect.ScreenRefresh -> {
                //no-op
            }
            is AllItemsViewEffect.OpenFile -> onOpenFile(
                consumedEffect.file,
                consumedEffect.mimeType
            )
            is AllItemsViewEffect.OpenWebpage -> onOpenWebpage(consumedEffect.uri)
            is AllItemsViewEffect.ShowPdf -> {
                onShowPdf(consumedEffect.file)
            }
            is AllItemsViewEffect.ShowVideoPlayer -> {
                navigateToVideoPlayerScreen()
            }
            is AllItemsViewEffect.ShowImageViewer -> {
                navigateToImageViewerScreen()
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
        BaseLceBox(
            modifier = Modifier.fillMaxSize(),
            lce = viewState.lce,
            error = { lceError ->
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
                CustomDivider()
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

        LongPressBottomSheet(
            layoutType = layoutType,
            longPressOptionsHolder = viewState.longPressOptionsHolder,
            onCollapse = viewModel::dismissBottomSheet,
            onOptionClick = viewModel::onLongPressOptionsItemSelected
        )
    }
}

@Composable
private fun AllItemsSearchBar(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel
) {
    val searchValue = viewState.searchTerm
    var searchBarTextFieldState by remember {
        mutableStateOf(
            TextFieldValue(
                searchValue ?: ""
            )
        )
    }
    val searchBarOnInnerValueChanged: (TextFieldValue) -> Unit = {
        searchBarTextFieldState = it
        viewModel.onSearch(it.text)
    }
    val onSearchAction = {
        searchBarOnInnerValueChanged.invoke(TextFieldValue())
    }

    SearchBar(
        hint = stringResource(id = Strings.search_items),
        modifier = Modifier.padding(12.dp),
        onSearchImeClicked = onSearchAction,
        onInnerValueChanged = searchBarOnInnerValueChanged,
        textFieldState = searchBarTextFieldState,
    )
}
