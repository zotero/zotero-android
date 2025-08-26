package org.zotero.android.screens.allitems

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.TopAppBarDefaults
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
import org.zotero.android.uicomponents.textinput.SearchViewM3

@Composable
internal fun AllItemsTabletSearchBar(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel
) {
    Column(modifier = Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets)) {
        Spacer(modifier = Modifier.height(8.dp))
        SearchViewM3Wrapper(
            text = viewState.searchTerm ?: "",
            onValueChanged = { viewModel.onSearch(it) },
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

}

@Composable
private fun SearchViewM3Wrapper(
    text: String?,
    onValueChanged: (String) -> Unit,
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

    SearchViewM3(
        hint = stringResource(id = Strings.items_search_title),
        onSearchImeClicked = onSearchAction,
        onInnerValueChanged = searchBarOnInnerValueChanged,
        textFieldState = searchBarTextFieldState,
    )
}