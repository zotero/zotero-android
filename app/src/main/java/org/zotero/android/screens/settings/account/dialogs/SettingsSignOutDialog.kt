package org.zotero.android.screens.settings.account.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3

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
        leftButtonColor = MaterialTheme.colorScheme.primary,
        leftButtonText = stringResource(id = Strings.no),
        rightButtonColor = MaterialTheme.colorScheme.primary,
        rightButtonText = stringResource(id = Strings.yes),
        onRightButtonClicked = onSignOut,
        onDismiss = onCancel
    )
}