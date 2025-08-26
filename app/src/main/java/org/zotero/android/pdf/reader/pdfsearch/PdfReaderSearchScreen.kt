package org.zotero.android.pdf.reader.pdfsearch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.reader.pdfsearch.row.PdfReaderSearchFoundMatchesRow
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun PdfReaderSearchScreen(
    viewModel: PdfReaderSearchViewModel,
    viewState: PdfReaderSearchViewState,
    onBack: () -> Unit,
) {
    viewModel.init()
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewEffect by viewModel.viewEffects.observeAsState()
    AppThemeM3(darkTheme = viewState.isDark) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is PdfReaderSearchViewEffect.OnBack -> {
                    onBack()
                }
            }
        }
        val layoutType = CustomLayoutSize.calculateLayoutType()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            if (layoutType.isTablet()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    PdfReaderSearchBar(
                        searchValue = viewState.searchTerm,
                        onSearch = viewModel::onSearch,
                    )
                }
            }

            if (viewState.searchResults.isNotEmpty()) {
                item {
                    PdfReaderSearchFoundMatchesRow(viewState)
                }
            }
            pdfReaderSearchTable(
                viewModel = viewModel,
                viewState = viewState,
            )

        }
    }
}