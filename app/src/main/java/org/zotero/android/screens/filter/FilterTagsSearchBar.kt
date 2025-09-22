package org.zotero.android.screens.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.SearchViewM3


@Composable
internal fun FilterTagsSearchBar(
    viewState: FilterViewState,
    viewModel: FilterViewModel,
    horizontalPadding: Dp = 12.dp,
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

    SearchViewM3(
        horizontalPadding = horizontalPadding,
        hint = stringResource(id = Strings.searchbar_placeholder),
        onSearchImeClicked = onSearchAction,
        onInnerValueChanged = searchBarOnInnerValueChanged,
        textFieldState = searchBarTextFieldState,
    )
}