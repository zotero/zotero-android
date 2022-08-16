package org.zotero.android.compose

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * This is the standard minimalistic text field.
 * It also shows an error if [errorText] is provided.
 *
 *
 * To align your text field properly use combination of [contentAlignment],
 * [horizontalAlignment] and text alignment inside the [textStyle] param.
 *
 * @param textStyle Style applied to both the text field and the hint
 * @param contentAlignment Aligns the actual text inside the text field
 * @param horizontalAlignment Aligns text field inside the column
 */
@Composable
fun CustomTextField(
    value: TextFieldValue,
    hint: String = "",
    onValueChange: (TextFieldValue) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    focusRequester: FocusRequester = remember { FocusRequester() },
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    maxCharacters: Int = Int.MAX_VALUE,
    errorText: String? = null,
    textColor: Color = Color.Black,
    hintColor: Color = Color.Unspecified
) {
    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusRequester.requestFocus()
                    }
                )
            },
        horizontalAlignment = horizontalAlignment
    ) {
        BasicTextField(
            value = value,
            onValueChange = {
                val boundedText = it.text.take(maxCharacters)
                onValueChange(it.copy(text = boundedText))
            },
            modifier = Modifier.focusRequester(focusRequester),
            enabled = enabled,
            textStyle = textStyle.copy(
                color = textColor
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            cursorBrush = SolidColor(LocalContentColor.current),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = contentAlignment
                ) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = hint,
                            color = hintColor.takeOrElse { Color.Blue },
                            style = textStyle
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (errorText != null) {
            Text(
                text = errorText,
                modifier = Modifier.padding(top = 8.dp),
                color = Color.Red,
            )
        }
    }
}

/**
 * This is the standard minimalistic text field
 * It also shows an error if [errorText] is provided.
 *
 * To align your text field properly use combination of [contentAlignment],
 * [horizontalAlignment] and text alignment inside the [textStyle] param.
 *
 * @param textStyle Style applied to both the text field and the hint
 * @param contentAlignment Aligns the actual text inside the text field
 * @param horizontalAlignment Aligns text field inside the column
 */
@Composable
fun CustomTextField(
    value: String,
    hint: String = "",
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    enabled: Boolean = true,
    errorText: String? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    hintColor: Color = Color.Unspecified,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    maxCharacters: Int = Int.MAX_VALUE,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = false,
    textColor: Color = Color.Black,
    textStyle: TextStyle = TextStyle.Default,
) {
    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusRequester.requestFocus()
                    }
                )
            },
        horizontalAlignment = horizontalAlignment
    ) {
        BasicTextField(
            value = value,
            onValueChange = {
                val boundedText = it.take(maxCharacters)
                onValueChange(boundedText)
            },
            modifier = Modifier.focusRequester(focusRequester),
            enabled = enabled,
            textStyle = textStyle.copy(
                color = textColor
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            cursorBrush = SolidColor(LocalContentColor.current),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = contentAlignment
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            color = hintColor.takeOrElse { Color.Black },
                            style = textStyle
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (errorText != null) {
            Text(
                text = errorText,
                modifier = Modifier.padding(top = 8.dp),
                color = Color.Red,
            )
        }
    }
}
