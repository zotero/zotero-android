package org.zotero.android.screens.settings.account.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.settings.account.SettingsAccountViewModel
import org.zotero.android.screens.settings.account.SettingsAccountViewState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.CustomOutlineTextField

@Composable
internal fun SettingsAccountFileSyncingPasswordItem(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    CustomOutlineTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        value = viewState.password,
        labelText = stringResource(id = Strings.settings_sync_password),
        placeholderText = stringResource(id = Strings.settings_sync_password),
        visualTransformation = PasswordVisualTransformation(),
        onValueChange = viewModel::setPassword,
        maxLines = 1,
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { viewModel.verify() }
        ),
        onEnterOrTab = {
            viewModel.verify()
        }
    )
}
