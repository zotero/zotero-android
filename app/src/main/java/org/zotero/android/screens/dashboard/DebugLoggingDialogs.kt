package org.zotero.android.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.architecture.logging.debug.DebugLoggingDialogData
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialog
import java.io.File

@Composable
internal fun DebugLoggingDialogs(
    dialogData: DebugLoggingDialogData,
    onDismissDialog: () -> Unit,
    onContentReadingRetry: (logs: List<File>, userId: Long, customAlertMessage: ((String) -> String)?) -> Unit,
    onContentReadingOk: () -> Unit,
    onUploadRetry: (logs: List<File>, userId: Long, customAlertMessage: ((String) -> String)?) -> Unit,
    onUploadOk: () -> Unit,
    onShareCopy: (String) -> Unit,
) {
    when (dialogData) {
        is DebugLoggingDialogData.start -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.errors_logging_title),
                description = stringResource(
                    id = Strings.errors_logging_start
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                ),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }

        is DebugLoggingDialogData.contentReading -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.errors_logging_title),
                description = stringResource(
                    id = Strings.errors_logging_content_reading
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = {
                        if (dialogData.shouldIncludeRetryAndOkProcessing) {
                            onContentReadingOk()
                        }

                    }
                ),
                secondaryAction = if (dialogData.shouldIncludeRetryAndOkProcessing) CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.retry),
                    onClick = { onContentReadingRetry(dialogData.logs, dialogData.userId, dialogData.customAlertMessage) }
                ) else null,
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }

        is DebugLoggingDialogData.cantCreateData -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.errors_logging_title),
                description = stringResource(
                    id = Strings.errors_logging_content_reading
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = {
                        onContentReadingOk()
                    }
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.retry),
                    onClick = { onContentReadingRetry(dialogData.logs, dialogData.userId, dialogData.customAlertMessage) }
                ),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }

        DebugLoggingDialogData.noLogsRecorded -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.errors_logging_title),
                description = stringResource(
                    id = Strings.errors_logging_no_logs_recorded
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = { }
                ),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }

        is DebugLoggingDialogData.upload -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.errors_logging_title),
                description = stringResource(
                    id = Strings.errors_logging_upload
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = { onUploadOk() }
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.retry),
                    onClick = { onUploadRetry(dialogData.logs, dialogData.userId, dialogData.customAlertMessage) }
                ),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }

        is DebugLoggingDialogData.responseParsing -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.errors_logging_title),
                description = stringResource(
                    id = Strings.errors_logging_response_parsing
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = { onUploadOk() }
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.retry),
                    onClick = { onUploadRetry(dialogData.logs, dialogData.userId, dialogData.customAlertMessage) }
                ),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }
        is DebugLoggingDialogData.share -> {
            val message = dialogData.customMessage ?: stringResource(
                id = Strings.settings_log_alert_message,
                dialogData.debugId
            )
            CustomAlertDialog(
                title = stringResource(id = Strings.settings_log_alert_title),
                description = message,
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.share_copy),
                    onClick = { onShareCopy(dialogData.debugId) }
                ),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }
    }
}