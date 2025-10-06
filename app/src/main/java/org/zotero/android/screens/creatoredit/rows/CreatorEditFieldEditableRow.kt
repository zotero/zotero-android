package org.zotero.android.screens.creatoredit.rows

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.creatoredit.CreatorEditViewModel
import org.zotero.android.uicomponents.textinput.CustomTextField

@Composable
internal fun CreatorEditFieldEditableRow(
    detailTitle: String,
    detailValue: String,
    viewModel: CreatorEditViewModel,
    layoutType: CustomLayoutSize.LayoutType,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onValueChange: (String) -> Unit,
    isLastField: Boolean,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    Row(
        modifier = Modifier
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .width(layoutType.calculateItemFieldLabelWidth())
        ) {
            Text(
                text = detailTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (isLastField) {
            CustomTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = detailValue,
                hint = "",
                textColor = textColor,
                onValueChange = onValueChange,
                maxLines = 1,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.onSave() }
                ),
                onEnterOrTab = { viewModel.onSave() },
                focusRequester = focusRequester,
            )
        } else {
            val focusManager = LocalFocusManager.current
            val moveFocusDownAction = {
                focusManager.moveFocus(FocusDirection.Down)
            }
            CustomTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = detailValue,
                hint = "",
                textColor = textColor,
                onValueChange = onValueChange,
                maxLines = 1,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                focusRequester = focusRequester,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { moveFocusDownAction() }
                ),
                onEnterOrTab = { moveFocusDownAction() },
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}