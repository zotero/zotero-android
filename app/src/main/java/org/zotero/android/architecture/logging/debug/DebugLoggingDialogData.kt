package org.zotero.android.architecture.logging.debug

import java.io.File

sealed class DebugLoggingDialogData {
    object start : DebugLoggingDialogData()
    data class contentReading(
        val shouldIncludeRetryAndOkProcessing: Boolean,
        val logs: List<File>,
        val userId: Long,
        val customAlertMessage: ((String) -> String)?
    ) : DebugLoggingDialogData()

    object noLogsRecorded : DebugLoggingDialogData()
    data class upload(
        val logs: List<File>,
        val userId: Long,
        val customAlertMessage: ((String) -> String)?
    ) : DebugLoggingDialogData()

    data class responseParsing(
        val logs: List<File>,
        val userId: Long,
        val customAlertMessage: ((String) -> String)?
    ) : DebugLoggingDialogData()

    data class cantCreateData(
        val logs: List<File>,
        val userId: Long,
        val customAlertMessage: ((String) -> String)?
    ) : DebugLoggingDialogData()

    data class share(val debugId: String, val customMessage: String?, val userId: Long) :
        DebugLoggingDialogData()

}
