package org.zotero.android.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.reader.data.ReaderFileType
import org.zotero.android.screens.reader.scrubber.ReaderPageHistoryArrowButton
import org.zotero.android.screens.reader.scrubber.ReaderPageIndicatorLabel
import org.zotero.android.screens.reader.scrubber.ReaderPageScrubber
import org.zotero.android.screens.reader.scrubber.ReaderScrubberViewModel
import org.zotero.android.screens.reader.toolbar.ReaderAnnotationCreationToolbar


@Composable
internal fun ReaderBox(
    viewModel: ReaderViewModel,
    viewState: ReaderViewState,
    scrubberViewModel: ReaderScrubberViewModel,
    isOverlayMode: Boolean,
) {
    var shouldShowSnapTargetAreas: Boolean by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Column(
        Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { layoutSize -> containerSize = layoutSize }
        ) {
            key(viewState.webViewGeneration) {
                ReaderWebView(viewModel = viewModel)
            }
            if (viewState.showCreationToolbar && containerSize != IntSize.Zero) {
                ReaderAnnotationCreationToolbar(
                    viewState = viewState,
                    viewModel = viewModel,
                    isOverlayMode = isOverlayMode,
                    position = viewState.toolbarPosition,
                    onPositionChange = viewModel::onToolbarPositionChange,
                    containerSize = containerSize,
                    onSnapTargetAreasVisibilityChange = { shouldShowSnapTargetAreas = it },
                    shouldShowSnapTargetAreas = shouldShowSnapTargetAreas
                )
            }
            if (viewState.fileType == ReaderFileType.PDF) {
                val isScrubberVisible = viewState.isScrubberVisible()
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (isScrubberVisible) 0.dp else 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ReaderPageIndicatorLabel(viewModel = scrubberViewModel, visible = isScrubberVisible)
                    AnimatedVisibility(visible = isScrubberVisible) {
                        ReaderPageScrubber(viewModel = scrubberViewModel)
                    }
                }
                if (isScrubberVisible) {
                    ReaderPageHistoryBackArrow(
                        visible = viewState.canNavigateBack,
                        onClick = { viewModel.navigatePageHistoryBack() },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                            .padding(start = 16.dp, bottom = ScrubberBottomClearance),
                    )
                    ReaderPageHistoryForwardArrow(
                        visible = viewState.canNavigateForward,
                        onClick = { viewModel.navigatePageHistoryForward() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                            .padding(end = 16.dp, bottom = ScrubberBottomClearance),
                    )
                }
            }
        }

        if (viewState.fileType == ReaderFileType.EPUB) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // EPUB: When bars are hidden, show page progress in the empty space
                val pageProgress = viewState.pageProgress
                if (!viewState.isTopBarVisible
                    && pageProgress != null
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = pageProgress,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private val ScrubberBottomClearance = 80.dp

@Composable
private fun ReaderPageHistoryBackArrow(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        ReaderPageHistoryArrowButton(isForward = false, onClick = onClick)
    }
}

@Composable
private fun ReaderPageHistoryForwardArrow(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        ReaderPageHistoryArrowButton(isForward = true, onClick = onClick)
    }
}

enum class ReaderToolbarPosition {
    Start,
    End,
    Top,
}

