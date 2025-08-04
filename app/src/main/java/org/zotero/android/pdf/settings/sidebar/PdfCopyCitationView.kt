package org.zotero.android.pdf.settings.sidebar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import org.zotero.android.pdf.reader.PdfReaderViewModel
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.screens.citation.singlecitation.SingleCitationScreen
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfCopyCitationView(
    viewState: PdfReaderViewState,
    viewModel: PdfReaderViewModel
) {
    AnimatedContent(
        targetState = viewState.showSingleCitationScreen,
        transitionSpec = {
            createCopyCitationTransitionSpec()
        }, label = ""
    ) { showView ->
        if (showView) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CustomTheme.colors.pdfAnnotationsFormBackground)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            //Prevent tap to be propagated to composables behind this screen.
                        }
                    }) {
                if (viewState.showSingleCitationScreen) {
                    SingleCitationScreen(
                        onBack = viewModel::hideCopyCitation
                    )
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<Boolean>.createCopyCitationTransitionSpec(): ContentTransform {
    val intOffsetSpec = tween<IntOffset>()
    return (slideInHorizontally(intOffsetSpec) { it } with
            slideOutHorizontally(intOffsetSpec) { it }).using(
        // Disable clipping since the faded slide-in/out should
        // be displayed out of bounds.
        SizeTransform(
            clip = false,
            sizeAnimationSpec = { _, _ -> tween() }
        ))
}