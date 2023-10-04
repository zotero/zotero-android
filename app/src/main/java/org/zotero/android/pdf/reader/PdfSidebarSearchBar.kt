package org.zotero.android.pdf.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.SearchBar
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfSidebarSearchBar(
    viewState: PdfReaderViewState,
    viewModel: PdfReaderViewModel
) {
    val searchValue = viewState.searchTerm
    var searchBarTextFieldState by remember {
        mutableStateOf(
            TextFieldValue(
                searchValue
            )
        )
    }
    val searchBarOnInnerValueChanged: (TextFieldValue) -> Unit = {
        searchBarTextFieldState = it
        viewModel.onSearch(it.text)
    }
    SearchBar(
        hint = stringResource(id = Strings.pdf_annotations_sidebar_search_title),
        onInnerValueChanged = searchBarOnInnerValueChanged,
        textFieldState = searchBarTextFieldState,
        backgroundColor = CustomTheme.colors.pdfAnnotationsSearchBarBackground
    )
}