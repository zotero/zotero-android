package org.zotero.android.pdf.reader

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.reader.toolbar.PdfReaderAnnotationCreationToolbar
import org.zotero.android.screens.allitems.ExportingAnnotatedPdfLoadingIndicator
import org.zotero.android.screens.allitems.GeneratingBibliographyLoadingIndicator


@Composable
internal fun PdfReaderPspdfKitBox(
    uri: Uri,
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState
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
        PdfReaderPspdfKitView(uri = uri, vMInterface = vMInterface)
        if (viewState.showCreationToolbar) {
            PdfReaderAnnotationCreationToolbar(
                viewState = viewState,
                vMInterface = vMInterface,
                state = anchoredDraggableState,
                onShowSnapTargetAreas = { shouldShowSnapTargetAreas = true },
                shouldShowSnapTargetAreas = shouldShowSnapTargetAreas
            )
        }
        if (viewState.isGeneratingBibliography) {
            GeneratingBibliographyLoadingIndicator()
        }
        if (viewState.isExportingAnnotatedPdf) {
            ExportingAnnotatedPdfLoadingIndicator()
        }
    }
}

enum class DragAnchors(val fraction: Float) {
    Start(0f),
    End(1f),
}

