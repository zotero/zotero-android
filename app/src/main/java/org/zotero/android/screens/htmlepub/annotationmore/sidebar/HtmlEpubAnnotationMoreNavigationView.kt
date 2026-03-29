package org.zotero.android.screens.htmlepub.annotationmore.sidebar

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
import org.zotero.android.screens.htmlepub.annotationmore.HtmlEpubAnnotationMoreNavigation
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewState

@Composable
internal fun HtmlEpubAnnotationMoreNavigationView(
    viewState: HtmlEpubReaderViewState,
    viewModel: HtmlEpubReaderViewModel
) {
    AnimatedContent(
        targetState = viewState.htmlEpubAnnotationMoreArgs != null,
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
                    }) {
                val args = viewState.htmlEpubAnnotationMoreArgs
                if (args != null) {
                    HtmlEpubAnnotationMoreNavigation(
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