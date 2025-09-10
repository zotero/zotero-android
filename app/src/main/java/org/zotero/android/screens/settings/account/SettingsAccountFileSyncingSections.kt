package org.zotero.android.screens.settings.account

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.zotero.android.api.network.CustomResult
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.account.dialogs.SettingsAccountWebDavOptionsDialog
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.screens.settings.elements.NewSettingsItemWithDescription
import org.zotero.android.screens.settings.elements.NewSettingsSectionTitle
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.webdav.data.FileSyncType
import org.zotero.android.webdav.data.WebDavError

@Composable
internal fun SettingsAccountFileSyncingSection(
    viewState: SettingsAccountViewState,
    viewModel: SettingsAccountViewModel
) {
    NewSettingsDivider()
    NewSettingsSectionTitle(titleId = Strings.settings_sync_file_syncing)
    SettingsAccountFileSyncingSyncMethodChooserItem(viewModel, viewState)
    if (viewState.fileSyncType == FileSyncType.webDav) {
        SettingsAccountFileSyncingWebDavItems(viewModel, viewState)
    }
}

@Composable
private fun SettingsAccountFileSyncingWebDavItems(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    SettingsDivider()
    SettingsAccountFileSyncingWebDavUrlItem(viewModel = viewModel, viewState = viewState)
    SettingsDivider()
    SettingsAccountFileSyncingUsernameItem(viewModel = viewModel, viewState = viewState)
    SettingsDivider()
    SettingsAccountFileSyncingPasswordItem(viewModel = viewModel, viewState = viewState)
    SettingsDivider()
    if (viewState.isVerifyingWebDav) {
        SettingsAccountFileSyncingVerificationInProgressItem(
            viewModel = viewModel,
            viewState = viewState
        )
    } else {
        SettingsAccountFileSyncingVerifyServerItem(viewModel = viewModel, viewState = viewState)
    }

    val webDavVerificationResult = viewState.webDavVerificationResult
    if (webDavVerificationResult is CustomResult.GeneralError) {
        SettingsDivider()
        SettingsAccountFileSyncingErrorMessageItem(webDavVerificationResult)
    }
}

@Composable
internal fun SettingsAccountFileSyncingSyncMethodChooserItem(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    if (viewState.showWebDavOptionsDialog) {
        SettingsAccountWebDavOptionsDialog(viewModel, viewState)
    }
    NewSettingsItemWithDescription(
        title = stringResource(id = Strings.settings_sync_file_syncing_type_message),
        description = stringResource(
            id = if (viewState.fileSyncType == FileSyncType.zotero) {
                Strings.file_syncing_zotero_option
            } else {
                Strings.file_syncing_webdav_option
            }
        ),
        onItemTapped = viewModel::showWebDavOptionsPopup
    )
}

@Composable
private fun SettingsAccountFileSyncingWebDavUrlItem(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    val focusManager = LocalFocusManager.current
    val moveFocusDownAction = {
        focusManager.moveFocus(FocusDirection.Down)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 84.dp)
                .align(Alignment.CenterStart)
        ) {
            Text(
                modifier = Modifier.safeClickable(
                    onClick = viewModel::showSchemaChooserScreen,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                ),
                text = viewState.scheme.name + "://",
                style = CustomTheme.typography.newBody,
                color = CustomTheme.colors.primaryContent,
            )
            Spacer(modifier = Modifier.width(8.dp))
            CustomTextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewState.url,
                hint = stringResource(id = Strings.file_syncing_url),
                onValueChange = viewModel::setUrl,
                textStyle = CustomTheme.typography.newBody,
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

        Text(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            text = "/zotero/",
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.primaryContent,
        )
    }
}

@Composable
private fun SettingsAccountFileSyncingUsernameItem(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    val focusManager = LocalFocusManager.current
    val moveFocusDownAction = {
        focusManager.moveFocus(FocusDirection.Down)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
    ) {
        CustomTextField(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth()
                .padding(start = 16.dp),
            value = viewState.username,
            hint = stringResource(id = Strings.settings_sync_username),
            onValueChange = viewModel::setUsername,
            textStyle = CustomTheme.typography.newBody,
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
}

@Composable
private fun SettingsAccountFileSyncingPasswordItem(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
    ) {
        CustomTextField(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth()
                .padding(start = 16.dp),
            value = viewState.password,
            hint = stringResource(id = Strings.settings_sync_password),
            visualTransformation = PasswordVisualTransformation(),
            onValueChange = viewModel::setPassword,
            maxLines = 1,
            textStyle = CustomTheme.typography.newBody,
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
}

@Composable
private fun SettingsAccountFileSyncingVerifyServerItem(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(CustomTheme.colors.surface)
            .safeClickable(
                onClick = viewModel::verify,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = viewState.canVerifyServer
            ),
    ) {
        val textColor = if (viewState.canVerifyServer) {
            CustomTheme.colors.zoteroBlueWithDarkMode
        } else {
            CustomTheme.colors.disabledContent
        }
        Text(
            modifier = Modifier
                .padding(start = 16.dp)
                .align(Alignment.CenterStart),
            text = stringResource(id = Strings.settings_sync_verify),
            style = CustomTheme.typography.newBody,
            color = textColor,
        )
        if (viewState.webDavVerificationResult is CustomResult.GeneralSuccess) {
            Row(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .align(Alignment.CenterEnd),
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                    text = stringResource(id = Strings.settings_sync_verified),
                    style = CustomTheme.typography.newBody,
                    color = CustomTheme.colors.primaryContent,
                )
                Icon(
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                    painter = painterResource(Drawables.check_24px),
                    contentDescription = null,
                    tint = CustomPalette.Green,
                )
            }

        }
    }
}

@Composable
private fun SettingsAccountFileSyncingErrorMessageItem(
    generalError: CustomResult.GeneralError,
) {
    val errorMessage = WebDavError.message(generalError)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = errorMessage,
            style = CustomTheme.typography.newBody,
            color = CustomPalette.ErrorRed,
        )
    }
}

@Composable
private fun SettingsAccountFileSyncingVerificationInProgressItem(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(CustomTheme.colors.surface)
    ) {
        CircularProgressIndicator(
            color = CustomTheme.colors.zoteroDefaultBlue,
            modifier = Modifier
                .padding(start = 16.dp)
                .align(Alignment.CenterStart)
                .size(24.dp)
        )
        Text(
            modifier = Modifier
                .padding(end = 16.dp)
                .align(Alignment.CenterEnd)
                .safeClickable(
                    onClick = viewModel::cancelVerification,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                ),
            text = stringResource(id = Strings.cancel),
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.zoteroBlueWithDarkMode,
        )
    }
}
