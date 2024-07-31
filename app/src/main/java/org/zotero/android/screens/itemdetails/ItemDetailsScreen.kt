package org.zotero.android.screens.itemdetails

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.bottomsheet.LongPressBottomSheet
import org.zotero.android.uicomponents.reorder.rememberReorderState
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars
import java.io.File

@Composable
internal fun ItemDetailsScreen(
    viewModel: ItemDetailsViewModel = hiltViewModel(),
    navigateToCreatorEdit: () -> Unit,
    navigateToTagPicker: () -> Unit,
    navigateToSinglePicker: () -> Unit,
    navigateToAddOrEditNote: () -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    navigateToZoterWebViewScreen: (String) -> Unit,
    onBack: () -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onShowPdf: (String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    onPickFile: () -> Unit,
) {
    CustomThemeWithStatusAndNavBars(statusBarBackgroundColor = CustomTheme.colors.topBarBackgroundColor) {

        val layoutType = CustomLayoutSize.calculateLayoutType()
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

                ItemDetailsViewEffect.ShowAddOrEditNoteEffect -> {
                    navigateToAddOrEditNote()
                }

                is ItemDetailsViewEffect.OpenFile -> {
                    onOpenFile(consumedEffect.file, consumedEffect.mimeType)
                }

                is ItemDetailsViewEffect.NavigateToPdfScreen -> {
                    onShowPdf(consumedEffect.params)
                }

                is ItemDetailsViewEffect.OpenWebpage -> {
                    onOpenWebpage(consumedEffect.uri)
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
        CustomScaffold(
            topBar = {
                ItemDetailsTopBar(
                    type = viewState.type,
                    onViewOrEditClicked = viewModel::onSaveOrEditClicked,
                    onCancelOrBackClicked = viewModel::onCancelOrBackClicked,
                    isEditing = viewState.isEditing
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = CustomTheme.colors.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                ) {
                    if (viewState.isEditing) {
                        ItemDetailsEditScreen(
                            viewState = viewState,
                            layoutType = layoutType,
                            viewModel = viewModel,
                            reorderState = reorderState,
                        )
                    } else {
                        ItemDetailsViewScreen(
                            viewState = viewState,
                            layoutType = layoutType,
                            viewModel = viewModel
                        )
                    }

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
            LongPressBottomSheet(
                layoutType = layoutType,
                longPressOptionsHolder = viewState.longPressOptionsHolder,
                onCollapse = viewModel::dismissBottomSheet,
                onOptionClick = viewModel::onLongPressOptionsItemSelected
            )
        }
    }
}
