package org.zotero.android.screens.reader.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeStringResource
import org.zotero.android.uicomponents.textinput.SearchViewM3

@Composable
internal fun ReaderSearchBar(
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
        hint = safeStringResource(id = Strings.pdf_search_title),
        onInnerValueChanged = searchBarOnInnerValueChanged,
        textFieldState = searchBarTextFieldState,
        focusOnScreenOpen = true
    )
}