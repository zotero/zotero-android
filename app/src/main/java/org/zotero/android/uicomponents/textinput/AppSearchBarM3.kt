package org.zotero.android.uicomponents.textinput

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables

@Composable
fun AppSearchBarM3(
    hint: String = "",
    onBack:() -> Unit,
    onSearchImeClicked: (() -> Unit)? = null,
    onInnerValueChanged: (TextFieldValue) -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textFieldState: TextFieldValue,
    focusOnScreenOpen: Boolean = false
) {

    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .height(64.dp),
        verticalAlignment = CenterVertically
    ) {
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(Drawables.arrow_back_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

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
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
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
                Box(
                    modifier = Modifier.padding(8.dp)
                ) {
                    if (textFieldState.text.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
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
