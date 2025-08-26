package org.zotero.android.screens.itemdetails.rows.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.textinput.CustomTextField

@Composable
internal fun ItemDetailsEditFieldEditableRow(
    key: String,
    fieldId: String,
    detailTitle: String,
    detailValue: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    isMultilineAllowed: Boolean = false,
    onValueChange: (String, String) -> Unit,
    onFocusChanges: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        Column(
            modifier = Modifier.width(layoutType.calculateItemFieldLabelWidth())
        ) {
            Text(
                modifier = Modifier.align(Alignment.End),
                text = detailTitle,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Column(modifier = Modifier.padding(start = 16.dp)) {
            if (isMultilineAllowed) {
                CustomTextField(
                    modifier = Modifier
                        .fillMaxSize()
                        .onFocusChanged {
                            if (it.hasFocus) {
                                onFocusChanges(fieldId)
                            }
                        },
                    value = detailValue,
                    hint = "",
                    textColor = textColor,
                    onValueChange = { onValueChange(key, it) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    ignoreTabsAndCaretReturns = false,
                )
            } else {
                val focusManager = LocalFocusManager.current
                val moveFocusDownAction = {
                    focusManager.moveFocus(FocusDirection.Down)
                }
                CustomTextField(
                    modifier = Modifier
                        .fillMaxSize()
                        .onFocusChanged {
                            if (it.hasFocus) {
                                onFocusChanges(fieldId)
                            }
                        },
                    value = detailValue,
                    hint = "",
                    textColor = textColor,
                    maxLines = 1,
                    onValueChange = { onValueChange(key, it) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { moveFocusDownAction() }
                    ),
                    onEnterOrTab = { moveFocusDownAction() },
                )
            }

        }
    }
}