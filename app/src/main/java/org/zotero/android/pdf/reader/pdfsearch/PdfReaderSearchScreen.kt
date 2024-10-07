package org.zotero.android.pdf.reader.pdfsearch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.foundation.quantityStringResource
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun PdfReaderSearchScreen(
    viewModel: PdfReaderSearchViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    viewModel.init()
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfReaderSearchViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    CustomThemeWithStatusAndNavBars(isDarkTheme = viewState.isDark) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is PdfReaderSearchViewEffect.OnBack -> {
                    onBack()
                }

            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                PdfReaderSearchBar(
                    searchValue = viewState.searchTerm,
                    onSearch = viewModel::onSearch,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            pdfReaderSearchTable(
                viewModel = viewModel,
                viewState = viewState,
            )

            item {
                if (viewState.searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = quantityStringResource(
                            id = Plurals.pdf_search_matches, viewState.searchResults.size
                        ),
                        style = CustomTheme.typography.newFootnote,
                        color = CustomPalette.DarkGrayColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}