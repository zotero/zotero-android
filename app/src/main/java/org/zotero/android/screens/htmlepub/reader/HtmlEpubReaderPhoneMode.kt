package org.zotero.android.screens.htmlepub.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchScreen
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchViewModel
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchViewState
import org.zotero.android.screens.htmlepub.reader.sidebar.HtmlEpubReaderSidebar

@Composable
internal fun HtmlEpubReaderPhoneMode(
    viewModel: HtmlEpubReaderViewModel,
    viewState: HtmlEpubReaderViewState,
    htmlEpubReaderSearchViewState: HtmlEpubReaderSearchViewState,
    htmlEpubReaderSearchViewModel: HtmlEpubReaderSearchViewModel,
    annotationsLazyListState: LazyListState,
    layoutType: CustomLayoutSize.LayoutType,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        HtmlEpubReaderBox(
            viewState = viewState,
            viewModel = viewModel,
        )

        AnimatedContent(
            targetState = viewState.showSideBar,
            transitionSpec = {
                htmlEpubReaderSidebarTransitionSpec()
            }, label = ""
        ) { showSideBar ->
            if (showSideBar) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                //Prevent tap to be propagated to composables behind this screen.
                            }
                        }) {
                    HtmlEpubReaderSidebar(
                        viewState = viewState,
                        viewModel = viewModel,
                        annotationsLazyListState = annotationsLazyListState,
                        layoutType = layoutType,
                    )
                }
            }
        }


        AnimatedContent(targetState = viewState.showPdfSearch, transitionSpec = {
            htmlEpubReaderPdfSearchTransitionSpec()
        }, label = "") { showScreen ->
            if (showScreen) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    HtmlEpubReaderSearchScreen(
                        onBack = viewModel::hidePdfSearch,
                        viewModel = htmlEpubReaderSearchViewModel,
                        viewState = htmlEpubReaderSearchViewState,
                    )
                }
            }
        }
    }
}
