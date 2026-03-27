package org.zotero.android.screens.htmlepub.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.reader.sidebar.SidebarDivider
import org.zotero.android.screens.htmlepub.reader.sidebar.HtmlEpubReaderSidebar

@Composable
internal fun HtmlEpubReaderTabletMode(
    viewModel: HtmlEpubReaderViewModel,
    viewState: HtmlEpubReaderViewState,
    annotationsLazyListState: LazyListState,
    layoutType: CustomLayoutSize.LayoutType,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(targetState = viewState.showSideBar, transitionSpec = {
            htmlEpubReaderSidebarTransitionSpec()
        }, label = "") { showSideBar ->
            if (showSideBar) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(330.dp)
                ) {
                    HtmlEpubReaderSidebar(
                        viewModel = viewModel,
                        viewState = viewState,
                        annotationsLazyListState = annotationsLazyListState,
                        layoutType = layoutType,
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

        HtmlEpubReaderBox(
            viewState = viewState,
            viewModel = viewModel,
        )
    }
}
