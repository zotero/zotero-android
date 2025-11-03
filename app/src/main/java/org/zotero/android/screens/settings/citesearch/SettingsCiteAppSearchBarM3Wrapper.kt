package org.zotero.android.screens.settings.citesearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.AppSearchBarM3

@Composable
internal fun SettingsCiteAppSearchBarM3Wrapper(
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
    )
}