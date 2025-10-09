package org.zotero.android.pdf.reader.pdfsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.SearchViewM3
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfReaderSearchBar(
    searchValue: String,
    onSearch: (String) -> Unit,
) {
    var searchBarTextFieldState by remember {
        mutableStateOf(
            TextFieldValue(
                text = searchValue,
                selection = TextRange(searchValue.length)
            )
        )
    }
    val searchBarOnInnerValueChanged: (TextFieldValue) -> Unit = {
        searchBarTextFieldState = it
        onSearch(it.text)
    }
    SearchViewM3(
        hint = stringResource(id = Strings.pdf_search_title),
        onInnerValueChanged = searchBarOnInnerValueChanged,
        textFieldState = searchBarTextFieldState,
        backgroundColor = CustomTheme.colors.pdfAnnotationsSearchBarBackground,
        focusOnScreenOpen = true
    )
}