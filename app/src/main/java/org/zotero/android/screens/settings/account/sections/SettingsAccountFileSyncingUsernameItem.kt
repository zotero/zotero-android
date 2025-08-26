package org.zotero.android.screens.settings.account.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.settings.account.SettingsAccountViewModel
import org.zotero.android.screens.settings.account.SettingsAccountViewState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.CustomOutlineTextField

@Composable
internal fun SettingsAccountFileSyncingUsernameItem(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    val focusManager = LocalFocusManager.current
    val moveFocusDownAction = {
        focusManager.moveFocus(FocusDirection.Down)
    }
    CustomOutlineTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        value = viewState.username,
        labelText = stringResource(id = Strings.settings_sync_username),
        placeholderText = stringResource(id = Strings.settings_sync_username),
        onValueChange = viewModel::setUsername,
        textStyle = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { moveFocusDownAction() }
        ),
        onEnterOrTab = { moveFocusDownAction() }
    )
}
