package org.zotero.android.screens.settings.account.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3ActionConfig

@Composable
internal fun SettingsSignOutDialog(
    onSignOut: () -> Unit,
    onCancel: () -> Unit,
) {
    CustomAlertDialogM3(
        title = stringResource(id = Strings.warning),
        description = stringResource(
            id = Strings.settings_logout_warning
        ),
        dismissButton = CustomAlertDialogM3ActionConfig(
            text = stringResource(id = Strings.no),
        ),
        confirmButton = CustomAlertDialogM3ActionConfig(
            text = stringResource(id = Strings.yes),
            onClick = onSignOut
        ),
        onDismiss = onCancel
    )
}