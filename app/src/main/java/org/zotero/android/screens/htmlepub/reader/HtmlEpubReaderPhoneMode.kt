package org.zotero.android.screens.htmlepub.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchScreen
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchViewModel
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchViewState

@Composable
internal fun HtmlEpubReaderPhoneMode(
    viewModel: HtmlEpubReaderViewModel,
    viewState: HtmlEpubReaderViewState,
    htmlEpubReaderSearchViewState: HtmlEpubReaderSearchViewState,
    htmlEpubReaderSearchViewModel: HtmlEpubReaderSearchViewModel,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        WebView(
            viewModel = viewModel
        )
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
