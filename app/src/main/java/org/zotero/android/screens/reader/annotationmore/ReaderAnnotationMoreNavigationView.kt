package org.zotero.android.screens.reader.annotationmore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import org.zotero.android.screens.reader.ReaderViewState
import org.zotero.android.screens.reader.ReaderViewModel

@Composable
internal fun ReaderAnnotationMoreNavigationView(
    viewState: ReaderViewState,
    viewModel: ReaderViewModel
) {
    AnimatedContent(
        targetState = viewState.readerAnnotationMoreArgs != null,
        transitionSpec = {
            createAnnotationTransitionSpec()
        }, label = ""
    ) { showView ->
        if (showView) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            //Prevent tap to be propagated to composables behind this screen.
                        }
                    }
            ) {
                val args = viewState.readerAnnotationMoreArgs
                if (args != null) {
                    ReaderAnnotationMoreNavigation(
                        args = args,
                        onBack = viewModel::hidePdfAnnotationMoreView
                    )
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<Boolean>.createAnnotationTransitionSpec(): ContentTransform {
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