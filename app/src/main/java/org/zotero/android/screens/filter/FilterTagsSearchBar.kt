package org.zotero.android.screens.filter

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.SearchBar

@Composable
internal fun RowScope.FilterTagsSearchBar(
    viewState: FilterViewState,
    viewModel: FilterViewModel
) {
    val searchValue = viewState.searchTerm
    var searchBarTextFieldState by remember {
        mutableStateOf(
            TextFieldValue(searchValue)
        )
    }
    val searchBarOnInnerValueChanged: (TextFieldValue) -> Unit = {
        searchBarTextFieldState = it
        viewModel.onSearch(it.text)
    }
    val onSearchAction = {
        searchBarOnInnerValueChanged.invoke(TextFieldValue())
    }

    SearchBar(
        hint = stringResource(id = Strings.searchbar_placeholder),
        modifier = Modifier
            .weight(1f)
            .padding(start = 16.dp, end = 12.dp),
        onSearchImeClicked = onSearchAction,
        onInnerValueChanged = searchBarOnInnerValueChanged,
        textFieldState = searchBarTextFieldState,
    )
}
