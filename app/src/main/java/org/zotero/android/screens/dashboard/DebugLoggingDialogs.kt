package org.zotero.android.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.architecture.logging.debug.DebugLoggingDialogData
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3ActionConfig
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
            CustomAlertDialogM3(
                title = stringResource(id = Strings.errors_logging_title),
                description = stringResource(
                    id = Strings.errors_logging_start
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(text = stringResource(id = Strings.ok)),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }

        is DebugLoggingDialogData.contentReading -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.errors_logging_title),
                description = stringResource(
                    id = Strings.errors_logging_content_reading
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = {
                        if (dialogData.shouldIncludeRetryAndOkProcessing) {
                            onContentReadingOk()
                        }

                    }),
                dismissButton = if (dialogData.shouldIncludeRetryAndOkProcessing) {
                    CustomAlertDialogM3ActionConfig(
                        text = stringResource(id = Strings.retry),
                        onClick = {
                            onContentReadingRetry(
                                dialogData.logs,
                                dialogData.userId,
                                dialogData.customAlertMessage
                            )
                        }
                    )
                } else {
                    null
                },
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }

        is DebugLoggingDialogData.cantCreateData -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.errors_logging_title),
                description = stringResource(
                    id = Strings.errors_logging_content_reading
                ),
                dismissButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = {
                        onContentReadingOk()
                    }),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.retry),
                    onClick = {
                        onContentReadingRetry(
                            dialogData.logs,
                            dialogData.userId,
                            dialogData.customAlertMessage
                        )
                    }
                ),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }

        DebugLoggingDialogData.noLogsRecorded -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.errors_logging_title),
                description = stringResource(
                    id = Strings.errors_logging_no_logs_recorded
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.ok),
                ),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }

        is DebugLoggingDialogData.upload -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.errors_logging_title),
                description = stringResource(
                    id = Strings.errors_logging_upload
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = { onUploadOk() }),
                dismissButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.retry),
                    onClick = {
                        onUploadRetry(
                            dialogData.logs,
                            dialogData.userId,
                            dialogData.customAlertMessage
                        )
                    }),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }

        is DebugLoggingDialogData.responseParsing -> {
            CustomAlertDialogM3(
                title = stringResource(id = Strings.errors_logging_title),
                description = stringResource(
                    id = Strings.errors_logging_response_parsing
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.ok),
                    onClick = { onUploadOk() }
                ),
                dismissButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.retry),
                    onClick = {
                        onUploadRetry(
                            dialogData.logs,
                            dialogData.userId,
                            dialogData.customAlertMessage
                        )
                    }
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
            CustomAlertDialogM3(
                title = stringResource(id = Strings.settings_log_alert_title),
                description = message,
                dismissButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.ok),
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = stringResource(id = Strings.copy_1),
                    onClick = { onShareCopy(dialogData.debugId) }
                ),
                dismissOnClickOutside = false,
                onDismiss = onDismissDialog
            )
        }
    }
}