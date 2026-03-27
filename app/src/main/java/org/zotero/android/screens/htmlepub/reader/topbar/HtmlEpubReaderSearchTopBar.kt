package org.zotero.android.screens.htmlepub.reader.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchViewModel
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchViewState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.AppSearchBarM3

@Composable
internal fun HtmlEpubReaderSearchTopBar(
    viewState: HtmlEpubReaderSearchViewState,
    viewModel: HtmlEpubReaderSearchViewModel,
    togglePdfSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
    ) {
        AppSearchBarM3Wrapper(
            text = viewState.searchTerm,
            onValueChanged = { viewModel.onSearch(it) },
            onBack = {
                viewModel.onSearch("")
                togglePdfSearch()
            }
        )
    }

}

@Composable
private fun AppSearchBarM3Wrapper(
    text: String?,
    onValueChanged: (String) -> Unit,
    onBack: () -> Unit,
) {
    val searchValue = text

    var searchBarTextFieldState by remember {
        mutableStateOf(
            TextFieldValue(
                searchValue ?: ""
            )
        )
    }
    val searchBarOnInnerValueChanged: (TextFieldValue) -> Unit = {
        searchBarTextFieldState = it
        onValueChanged(it.text)
    }
    val onSearchAction = {
        //no-op
    }

    AppSearchBarM3(
        hint = stringResource(id = Strings.pdf_search_title),
        onSearchImeClicked = onSearchAction,
        onInnerValueChanged = searchBarOnInnerValueChanged,
        textFieldState = searchBarTextFieldState,
        onBack = onBack,
        focusOnScreenOpen = true,
    )
}