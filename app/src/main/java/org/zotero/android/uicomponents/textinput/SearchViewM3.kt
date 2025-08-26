package org.zotero.android.uicomponents.textinput

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables

@Composable
fun SearchViewM3(
    modifier: Modifier = Modifier,
    hint: String = "",
    onSearchImeClicked: (() -> Unit)? = null,
    onInnerValueChanged: (TextFieldValue) -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textFieldState: TextFieldValue,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    focusOnScreenOpen: Boolean = false,
    horizontalPadding: Dp = 12.dp,
) {
    val cornerShape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .clip(cornerShape)
            .background(
                color = backgroundColor,
                shape = cornerShape)
            .heightIn(48.dp),
        verticalAlignment = CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            painter = painterResource(id = Drawables.search_24px),
            contentDescription = null,
            modifier = Modifier.padding(),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusRequester = remember { FocusRequester() }
        if (focusOnScreenOpen) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        BasicTextField(
            value = textFieldState,
            onValueChange = onInnerValueChanged,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            textStyle = textStyle.copy(
                color = MaterialTheme.colorScheme.onSurface
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
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
//                Box(
////                    modifier = Modifier.padding(8.dp)
//                ) {
                    if (textFieldState.text.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
//                }
            },
            singleLine = true,
        )
        if (textFieldState.text.isNotEmpty()) {
            IconButton(onClick = { onInnerValueChanged.invoke(TextFieldValue()) }) {
                Icon(
                    painter = painterResource(Drawables.ic_close_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
