package org.zotero.android.screens.htmlepub.reader

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderDocumentType
import org.zotero.android.screens.htmlepub.reader.web.actionmenu.HtmlEpubActionMenuPopup
import org.zotero.android.screens.htmlepub.toolbar.HtmlEpubReaderAnnotationCreationToolbar


@Composable
internal fun HtmlEpubReaderBox(
    viewModel: HtmlEpubReaderViewModel,
    viewState: HtmlEpubReaderViewState,
) {
    val density = LocalDensity.current
    val positionalThreshold = { distance: Float -> distance * 0.5f }
    val velocityThreshold = { with(density) { 1000.dp.toPx() } }
    val animationSpec = tween<Float>()

    var shouldShowSnapTargetAreas: Boolean by remember { mutableStateOf(false) }
    val confirmValueChange = { newValue: DragAnchors ->
        shouldShowSnapTargetAreas = false
        true
    }
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val anchoredDraggableState = rememberSaveable(
        saver = AnchoredDraggableState.Saver(
            snapAnimationSpec = animationSpec,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            confirmValueChange = confirmValueChange,
            decayAnimationSpec = decayAnimationSpec
        )
    ) {
        AnchoredDraggableState(
            initialValue = DragAnchors.Start,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            snapAnimationSpec = animationSpec,
            decayAnimationSpec = decayAnimationSpec,
            confirmValueChange = confirmValueChange,
        )
    }
    val rightTargetAreaXOffset = with(density) { 92.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        val selectedTextParamsRects = viewState.selectedTextParamsRects
        if (selectedTextParamsRects != null) {
            HtmlEpubActionMenuPopup(
                selectedTextParamsRects = selectedTextParamsRects,
                topInset = viewState.containerInsetTop,
                leftInset = viewState.containerInsetLeft,
                viewModel = viewModel,
            )
        }

        val snapshotTopBarPadding by animateDpAsState(
            targetValue = if (viewState.isTopBarVisible) {
                TopAppBarDefaults.TopAppBarExpandedHeight
            } else {
                0.dp
            },
            label = "snapshotTopBarPadding",
        )
        val webViewModifier = if (viewState.type == HtmlEpubReaderDocumentType.EPUB) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = snapshotTopBarPadding)
        }
        Box(modifier = webViewModifier) {
            HtmlEpubReaderWebView(viewModel)
        }
        if (viewState.showCreationToolbar) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                    .onSizeChanged { layoutSize ->
                        val dragEndPoint = layoutSize.width - rightTargetAreaXOffset
                        anchoredDraggableState.updateAnchors(
                            DraggableAnchors {
                                DragAnchors.entries
                                    .forEach { anchor ->
                                        anchor at dragEndPoint * anchor.fraction
                                    }
                            }
                        )
                    }
            ) {
                HtmlEpubReaderAnnotationCreationToolbar(
                    viewState = viewState,
                    viewModel = viewModel,
                    state = anchoredDraggableState,
                    onShowSnapTargetAreas = { shouldShowSnapTargetAreas = true },
                    shouldShowSnapTargetAreas = shouldShowSnapTargetAreas
                )
            }
        }

        // EPUB: When bars are hidden, show page progress in the empty space
        val pageProgress = viewState.pageProgress
        if (viewState.type == HtmlEpubReaderDocumentType.EPUB
            && !viewState.isTopBarVisible
            && pageProgress != null
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
                    .height(TopAppBarDefaults.TopAppBarExpandedHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = pageProgress,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

enum class DragAnchors(val fraction: Float) {
    Start(0f),
    End(1f),
}

