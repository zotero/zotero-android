package org.zotero.android.pdf.reader

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.reader.sidebar.PdfReaderSidebar
import org.zotero.android.pdf.reader.sidebar.SidebarDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfReaderTabletMode(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    lazyListState: LazyListState,
    layoutType: CustomLayoutSize.LayoutType,
    uri: Uri,
    focusRequester: FocusRequester,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(targetState = viewState.showSideBar, transitionSpec = {
            createSidebarTransitionSpec()
        }, label = "") { showSideBar ->
            if (showSideBar) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.35f)
                        .background(CustomTheme.colors.pdfAnnotationsFormBackground)
                ) {
                    PdfReaderSidebar(
                        vMInterface = vMInterface,
                        viewState = viewState,
                        lazyListState = lazyListState,
                        layoutType = layoutType,
                        focusRequester = focusRequester,
                    )
                }
            }
        }
        if (viewState.showSideBar) {
            SidebarDivider(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
            )
        }

        PdfReaderPspdfKitBox(
            uri = uri,
            viewState = viewState,
            vMInterface = vMInterface,
        )
    }
}

@Composable
internal fun PdfReaderPhoneMode(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    lazyListState: LazyListState,
    layoutType: CustomLayoutSize.LayoutType,
    uri: Uri,
    focusRequester: FocusRequester,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PdfReaderPspdfKitBox(
            uri = uri,
            viewState = viewState,
            vMInterface = vMInterface
        )
        AnimatedContent(
            targetState = viewState.showSideBar,
            transitionSpec = {
                createSidebarTransitionSpec()
            }, label = ""
        ) { showSideBar ->
            if (showSideBar) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CustomTheme.colors.pdfAnnotationsFormBackground)
                ) {
                    PdfReaderSidebar(
                        viewState = viewState,
                        vMInterface = vMInterface,
                        lazyListState = lazyListState,
                        layoutType = layoutType,
                        focusRequester = focusRequester
                    )
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<Boolean>.createSidebarTransitionSpec(): ContentTransform {
    val intOffsetSpec = tween<IntOffset>()
    return (slideInHorizontally(intOffsetSpec) { -it } with
            slideOutHorizontally(intOffsetSpec) { -it }).using(
        // Disable clipping since the faded slide-in/out should
        // be displayed out of bounds.
        SizeTransform(
            clip = false,
            sizeAnimationSpec = { _, _ -> tween() }
        ))
}
