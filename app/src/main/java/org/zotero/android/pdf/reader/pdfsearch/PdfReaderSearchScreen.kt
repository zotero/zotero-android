package org.zotero.android.pdf.reader.pdfsearch

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.pdf.reader.pdfsearch.data.PdfReaderSearchItem
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.quantityStringResource
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.textinput.SearchBar
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

            PdfReaderSearchTable(
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

internal fun LazyListScope.PdfReaderSearchTable(
    viewState: PdfReaderSearchViewState,
    viewModel: PdfReaderSearchViewModel,
) {
    items(viewState.searchResults) { item ->
        PdfReaderSearchRow(searchItem = item, onItemTapped = {viewModel.onItemTapped(item)})
    }

}

@Composable
private fun PdfReaderSearchRow(
    searchItem: PdfReaderSearchItem,
    onItemTapped: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onItemTapped,
            )
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier.align(Alignment.End),
            text = stringResource(Strings.page) + " ${searchItem.pageNumber + 1}",
            style = CustomTheme.typography.newCaptionOne,
            color = CustomTheme.colors.zoteroBlueWithDarkMode
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier,
            text = searchItem.annotatedString,
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.defaultTextColor,
        )
        Spacer(modifier = Modifier.height(8.dp))
        NewDivider(
            modifier = Modifier
        )

    }
}

@Composable
internal fun PdfReaderSearchBar(
    searchValue: String,
    onSearch: (String) -> Unit,
) {
    var searchBarTextFieldState by remember {
        mutableStateOf(
            TextFieldValue(
                searchValue
            )
        )
    }
    val searchBarOnInnerValueChanged: (TextFieldValue) -> Unit = {
        searchBarTextFieldState = it
        onSearch(it.text)
    }
    SearchBar(
        modifier = Modifier.padding(horizontal = 16.dp),
        hint = stringResource(id = Strings.pdf_search_title),
        onInnerValueChanged = searchBarOnInnerValueChanged,
        textFieldState = searchBarTextFieldState,
        backgroundColor = CustomTheme.colors.pdfAnnotationsSearchBarBackground
    )
}
