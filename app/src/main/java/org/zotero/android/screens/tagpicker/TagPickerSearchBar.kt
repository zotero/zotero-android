package org.zotero.android.screens.tagpicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.SearchViewM3

@Composable
internal fun TagPickerSearchBar(
    onSearchAction: (() -> Unit)? = null,
    searchBarOnInnerValueChanged: (TextFieldValue) -> Unit,
    searchBarTextFieldState: TextFieldValue,
    horizontalPadding: Dp = 12.dp,
) {

    SearchViewM3(
        horizontalPadding = horizontalPadding,
        hint = stringResource(id = Strings.tag_picker_placeholder),
        onSearchImeClicked = onSearchAction,
        onInnerValueChanged = searchBarOnInnerValueChanged,
        textFieldState = searchBarTextFieldState,
    )
}