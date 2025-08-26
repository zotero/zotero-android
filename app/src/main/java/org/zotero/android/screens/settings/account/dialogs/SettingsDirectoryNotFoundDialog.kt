package org.zotero.android.screens.settings.account.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3ActionConfig

@Composable
internal fun SettingsDirectoryNotFoundDialog(
    url: String,
    onCreate: () -> Unit,
    onCancel: () -> Unit,
) {
    CustomAlertDialogM3(
        title = stringResource(id = Strings.settings_sync_directory_not_found_title),
        description = stringResource(
            id = Strings.settings_sync_directory_not_found_message, url
        ),
        dismissButton = CustomAlertDialogM3ActionConfig(
            text = stringResource(id = Strings.cancel),
        ),
        confirmButton = CustomAlertDialogM3ActionConfig(
            text = stringResource(id = Strings.create),
            onClick = onCreate
        ),
        onDismiss = onCancel
    )
}
