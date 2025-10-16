package org.zotero.android.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.architecture.logging.crash.CrashReportIdDialogData
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3ActionConfig

@Composable
internal fun CrashLoggingDialogs(
    dialogData: CrashReportIdDialogData,
    onDismissDialog: () -> Unit,
    onShareCopy: (String) -> Unit,
) {
    CustomAlertDialogM3(
        title = stringResource(id = Strings.settings_crash_alert_title),
        description = stringResource(
            id = Strings.settings_crash_alert_message, dialogData.reportId
        ),
        dismissButton = CustomAlertDialogM3ActionConfig(text = stringResource(id = Strings.ok)),
        confirmButton = CustomAlertDialogM3ActionConfig(
            text = stringResource(id = Strings.settings_crash_alert_copy_id),
            onClick = { onShareCopy(dialogData.reportId) }),
        dismissOnClickOutside = false,
        onDismiss = onDismissDialog
    )
}