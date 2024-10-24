package org.zotero.android.uicomponents.textinput

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.button.TextFieldClearButton
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    hint: String = "",
    onSearchImeClicked: (() -> Unit)? = null,
    onInnerValueChanged: (TextFieldValue) -> Unit,
    textStyle: TextStyle = CustomTheme.typography.newBody,
    textFieldState: TextFieldValue,
    backgroundColor: Color = CustomTheme.colors.inputBar,
    focusOnScreenOpen: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor),
        verticalAlignment = CenterVertically
    ) {
        Icon(
            painter = painterResource(id = Drawables.search_24px),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 0.dp, bottom = 8.dp),
            tint = CustomTheme.colors.secondaryContent
        )
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusRequester = remember { FocusRequester() }
        if (focusOnScreenOpen) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        BasicTextField(
            value = textFieldState,
            onValueChange = onInnerValueChanged,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            textStyle = textStyle.copy(
                color = CustomTheme.colors.primaryContent
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    onSearchImeClicked?.invoke()
                }
            ),
            cursorBrush = SolidColor(LocalContentColor.current),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.padding(8.dp)
                ) {
                    if (textFieldState.text.isEmpty()) {
                        Text(
                            text = hint,
                            color = CustomPalette.CoolGray,
                            style = textStyle
                        )
                    }
                    innerTextField()
                }
            },
            singleLine = true,
        )
        if (textFieldState.text.isNotEmpty()) {
            TextFieldClearButton(
                contentDescription = stringResource(id = Strings.searchbar_accessibility_clear),
                contentScale = ContentScale.None,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(24.dp),
                onClick = { onInnerValueChanged.invoke(TextFieldValue()) },
            )
        }
    }
}
