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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.button.TextFieldClearButton
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@OptIn(ExperimentalComposeUiApi::class)
@Preview(widthDp = 320)
@Composable
fun SearchBarEmptyPreview() {
    CustomTheme {
        SearchBar(hint = "Search items")
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Preview(widthDp = 320, showBackground = true)
@Composable
fun SearchBarPreview() {
    CustomTheme(isDarkTheme = true) {
        SearchBar(hint = "Search items", value = "Sustainable agriculture")
    }
}

@ExperimentalComposeUiApi
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    hint: String = "",
    value: String = "",
    onValueChange: (String) -> Unit = {},
    onSearchImeClicked: (() -> Unit)? = null,
    textStyle: TextStyle = CustomTheme.typography.default,
    maxCharacters: Int = Int.MAX_VALUE,
) {
    var textFieldState by remember { mutableStateOf(TextFieldValue(value)) }

    val onInnerValueChanged: (TextFieldValue) -> Unit = {
        if (it.text.length <= maxCharacters) {
            textFieldState = it
            onValueChange(it.text)
        } else {
            // If we have text exceeding maxCharacters, this cuts the excessive characters
            // and puts cursor position to the end of the field for convenience
            val boundedText = it.text.take(maxCharacters)
            val boundedTextFieldValue = TextFieldValue(
                text = boundedText,
                selection = TextRange(boundedText.length)
            )
            textFieldState = boundedTextFieldValue
            onValueChange(boundedTextFieldValue.text)
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CustomTheme.colors.inputBar),
        verticalAlignment = CenterVertically
    ) {
        Icon(
            painter = painterResource(id = Drawables.ic_search_24dp),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 0.dp, bottom = 8.dp),
            tint = CustomTheme.colors.secondaryContent
        )
        val keyboardController = LocalSoftwareKeyboardController.current
        BasicTextField(
            value = textFieldState,
            onValueChange = onInnerValueChanged,
            modifier = Modifier.weight(1f),
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
                contentDescription = stringResource(id = Strings.clear_search),
                contentScale = ContentScale.None,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(24.dp),
                onClick = { onInnerValueChanged.invoke(TextFieldValue()) },
            )
        }
    }
}
