package org.zotero.android.screens.itemdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.itemdetails.topbars.ItemDetailsEditingTopBar
import org.zotero.android.screens.itemdetails.topbars.ItemDetailsTopBar
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.bottomsheet.LongPressBottomSheetM3
import org.zotero.android.uicomponents.reorder.rememberReorderState
import org.zotero.android.uicomponents.themem3.AppThemeM3
import java.io.File

@Composable
internal fun ItemDetailsScreen(
    viewModel: ItemDetailsViewModel = hiltViewModel(),
    navigateToCreatorEdit: () -> Unit,
    navigateToTagPicker: () -> Unit,
    navigateToSinglePicker: () -> Unit,
    navigateToAddOrEditNote: (String) -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    navigateToZoterWebViewScreen: (String) -> Unit,
    onBack: () -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onShowPdf: (String) -> Unit,
    onOpenWebpage: (url: String) -> Unit,
    onPickFile: () -> Unit,
) {
    AppThemeM3 {

        val viewState by viewModel.viewStates.observeAsState(ItemDetailsViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()

        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }
        val lazyListState = rememberLazyListState()

        val reorderState = rememberReorderState(
            listState = lazyListState
        )

        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                null -> Unit
                ItemDetailsViewEffect.ShowCreatorEditEffect -> {
                    navigateToCreatorEdit()
                }

                ItemDetailsViewEffect.ShowItemTypePickerEffect -> {
                    navigateToSinglePicker()
                }

                ItemDetailsViewEffect.ScreenRefresh -> {
                    //no-op
                }

                ItemDetailsViewEffect.OnBack -> {
                    onBack()
                }

                is ItemDetailsViewEffect.ShowAddOrEditNoteEffect -> {
                    navigateToAddOrEditNote(consumedEffect.screenArgs)
                }

                is ItemDetailsViewEffect.OpenFile -> {
                    onOpenFile(consumedEffect.file, consumedEffect.mimeType)
                }

                is ItemDetailsViewEffect.NavigateToPdfScreen -> {
                    onShowPdf(consumedEffect.params)
                }

                is ItemDetailsViewEffect.OpenWebpage -> {
                    onOpenWebpage(consumedEffect.url)
                }

                is ItemDetailsViewEffect.ShowVideoPlayer -> {
                    navigateToVideoPlayerScreen()
                }

                is ItemDetailsViewEffect.ShowImageViewer -> {
                    navigateToImageViewerScreen()
                }

                is ItemDetailsViewEffect.ShowZoteroWebView -> {
                    navigateToZoterWebViewScreen(consumedEffect.url)
                }

                is ItemDetailsViewEffect.AddAttachment -> {
                    onPickFile()
                }

                ItemDetailsViewEffect.ShowTagPickerEffect -> {
                    navigateToTagPicker()
                }

            }
        }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        CustomScaffoldM3(
            scrollBehavior = scrollBehavior,
            topBar = {
                if (viewState.isEditing) {
                    ItemDetailsEditingTopBar(
                        type = viewState.type,
                        onViewOrEditClicked = viewModel::onSaveOrEditClicked,
                        onCancelOrBackClicked = viewModel::onCancelOrBackClicked,
                    )
                } else {
                    ItemDetailsTopBar(
                        onViewOrEditClicked = viewModel::onSaveOrEditClicked,
                        onCancelOrBackClicked = viewModel::onCancelOrBackClicked,
                        scrollBehavior = scrollBehavior,
                    )
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (viewState.isEditing) {
                    ItemDetailsEditScreen(
                        viewState = viewState,
                        viewModel = viewModel,
                        reorderState = reorderState,
                    )
                } else {
                    ItemDetailsViewScreen(
                        viewState = viewState,
                        viewModel = viewModel
                    )
                }
            }
            val itemDetailError = viewState.error
            if (itemDetailError != null) {
                ItemDetailsErrorDialogs(
                    itemDetailError = itemDetailError,
                    onDismissErrorDialog = viewModel::onDismissErrorDialog,
                    onBack = onBack,
                    acceptPrompt = viewModel::acceptPrompt,
                    cancelPrompt = viewModel::cancelPrompt,
                    acceptItemWasChangedRemotely = viewModel::acceptItemWasChangedRemotely,
                    deleteOrRestoreItem = viewModel::deleteOrRestoreItem
                )
            }
            LongPressBottomSheetM3(
                longPressOptionsHolder = viewState.longPressOptionsHolder,
                onCollapse = viewModel::dismissBottomSheet,
                onOptionClick = viewModel::onLongPressOptionsItemSelected
            )
        }
    }
}
