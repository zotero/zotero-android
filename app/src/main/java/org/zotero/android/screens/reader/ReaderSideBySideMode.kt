package org.zotero.android.screens.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.reader.sidebar.SidebarDivider
import org.zotero.android.screens.reader.sidebar.ReaderSidebar

@Composable
internal fun ReaderSideBySideMode(
    viewModel: ReaderViewModel,
    viewState: ReaderViewState,
    annotationsLazyListState: LazyListState,
    annotationMaxSideSize: Int,
) {
    val isPdfOrHtml = viewState.isPdfOrHtml()

    var rowModifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(BottomAppBarDefaults.windowInsets)
    if (!isPdfOrHtml) {
        rowModifier = rowModifier
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            )
    }

    Row(modifier = rowModifier) {

        AnimatedContent(
            targetState = viewState.showSideBar,
            transitionSpec = {
                readerSidebarTransitionSpec()
            }, label = ""
        ) { showSideBar ->
            if (showSideBar) {
                Column(
                    modifier = Modifier
                        .width(330.dp)
                        .fillMaxHeight()
                ) {
                    ReaderSidebar(
                        viewModel = viewModel,
                        viewState = viewState,
                        annotationsLazyListState = annotationsLazyListState,
                        annotationMaxSideSize = annotationMaxSideSize,
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
        ReaderBox(
            viewState = viewState,
            viewModel = viewModel,
            isOverlayMode = false
        )
    }
}
