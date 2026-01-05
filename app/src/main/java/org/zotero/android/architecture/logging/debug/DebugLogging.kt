package org.zotero.android.architecture.logging.debug

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.zotero.android.api.NonZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.logging.DeviceInfoProvider
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugLogging @Inject constructor(
    dispatcher: CoroutineDispatcher,
    private val debugLoggingTree: DebugLoggingTree,
    private val fileStore: FileStore,
    private val debugLoggingDialogDataEventStream: DebugLoggingDialogDataEventStream,
    private val nonZoteroApi: NonZoteroApi,
    private val defaults: Defaults
) : DebugLogFormatterInterface {
    enum class LoggingType {
        immediate,
        nextLaunch
    }

    sealed class Error : Exception() {
        object start : Error()
        object contentReading : Error()
        object noLogsRecorded : Error()
        object upload : Error()
        object responseParsing : Error()
        object cantCreateData : Error()
    }

    private var coroutineScope = CoroutineScope(dispatcher)

    var isEnabled: Boolean
        set(newValue) {
            defaults.setDebugLogEnabled(newValue)
            this.debugLoggingInterface?.setDebugWindow(newValue)
        }
        get() {
            return defaults.isDebugLogEnabled()
        }

    private var didStartFromLaunch: Boolean = false
    private var logger: DebugFileWriter? = null

    var debugLoggingInterface: DebugLoggingInterface? = null
        set(newValue) {
            field = newValue
            val action = this.pendingAction ?: return
            coroutineScope.launch {
                shareLogs(
                    ignoreEmptyLogs = action.ignoreEmptyLogs,
                    userId = action.userId,
                    customAlertMessage = action.customAlertMessage
                )
                this@DebugLogging.logger = null
            }
        }
    private var pendingAction: PendingCoordinatorAction? = null
    val logString = MutableStateFlow("")
    val logLines = MutableStateFlow(0)

    fun start(type: LoggingType) {
        this.isEnabled = true
        if (type == LoggingType.immediate) {
            startLogger()
        }
    }

    fun stop(
        ignoreEmptyLogs: Boolean = false,
        userId: Long = 0,
        customAlertMessage: ((String) -> String)? = null
    ) {
        this.isEnabled = false
        this.didStartFromLaunch = false

        val logger = this.logger ?: return
        debugLoggingTree.setDebugFileWriter(null)
        this.logString.tryEmit("")
        this.logLines.tryEmit(0)

        logger.flushAndClose()

        if (this.debugLoggingInterface == null) {
            this.pendingAction = PendingCoordinatorAction(
                ignoreEmptyLogs = ignoreEmptyLogs,
                userId = userId,
                customAlertMessage = customAlertMessage
            )
            return
        }

        coroutineScope.launch {
            shareLogs(
                ignoreEmptyLogs = ignoreEmptyLogs,
                userId = userId,
                customAlertMessage = customAlertMessage
            )
            this@DebugLogging.logger = null
        }
    }

    fun cancel(completed: (() -> Unit)? = null) {
        this.isEnabled = false
        this.didStartFromLaunch = false

        val logger = this.logger ?: return
        debugLoggingTree.setDebugFileWriter(null)
        this.logString.tryEmit("")
        this.logLines.tryEmit(0)
        logger.flushAndClose()

        coroutineScope.launch {
            clearDebugDirectory()
            this@DebugLogging.logger = null

            if (completed != null) {
                completed()
            }
        }
    }

    fun startLoggingOnLaunchIfNeeded() {
        if (!this.isEnabled) {
            return
        }
        this.didStartFromLaunch = true
        startLogger()
    }

    fun storeLogs(completed: () -> Void) {
        val logger = this.logger
        if (logger == null) {
            completed()
            return
        }
        logger.flushAndClose()
        completed()
    }

    private fun clearDebugDirectory() {
        try {
            FileHelper.deleteFolder(fileStore.debugLoggingDirectory())
        } catch (error: Exception) {
            Timber.e(error, "DebugLogging: can't delete directory")
        }
    }

    private fun startLogger() {
        if (this.logger != null) {
            return
        }
        try {
            val file = fileStore.debugLoggingDirectory()
            try {
                FileHelper.deleteFolder(file)
            } catch (e: Exception) {
                //no-op
            }
            file.mkdirs()
            val debugFileWriter = DebugFileWriter(fileStore = fileStore)
            debugLoggingTree.setDebugFileWriter(debugFileWriter)
            debugLoggingTree.setDebugLogFormatterInterface(this)
            this.logger = debugFileWriter
        } catch (error: Exception) {
            Timber.e(error, "DebugLogging: can't start logger")
            debugLoggingDialogDataEventStream.emitAsync(DebugLoggingDialogData.start)
        }
    }

    override fun didFormat(message: String) {
        if (this.logString.value.isEmpty()) {
            this.logString.tryEmit(message)
        } else {
            this.logString.tryEmit("${message}\n\n${this.logString.value}")
        }
        this.logLines.tryEmit(this.logLines.value + 1)
    }

    fun onContentReadingRetry(
        logs: List<File>,
        userId: Long,
        customAlertMessage: ((String) -> String)?
    ) {
        coroutineScope.launch {
            submit(logs = logs, userId = userId, customAlertMessage = customAlertMessage)
        }
    }

    fun onContentReadingOk() {
        clearDebugDirectory()
    }

    fun onUploadRetry(logs: List<File>, userId: Long, customAlertMessage: ((String) -> String)?) {
        coroutineScope.launch {
            submit(logs = logs, userId = userId, customAlertMessage = customAlertMessage)
        }
    }

    fun onUploadOk() {
        clearDebugDirectory()
    }

    private fun data(logs: List<File>): String {
        val timestamp = System.currentTimeMillis()
        var allLogs = "\n\n" + DeviceInfoProvider.debugString + "\nTimestamp: $timestamp" + "\n\n\n"
        try {
            for (url in logs) {
                val string = FileHelper.readFileToString(url)
                allLogs += string
            }
        } catch (e: Exception) {
            Timber.e(e)
            throw Error.cantCreateData
        }
        return allLogs
    }

    private suspend fun submit(
        logs: List<File>,
        userId: Long,
        customAlertMessage: ((String) -> String)?
    ) {
        val dataToPost: String
        try {
            dataToPost = data(logs)
        } catch (error: Exception) {
            Timber.e(error, "DebugLogging: can't read all logs")
            if (error is Error.cantCreateData) {
                debugLoggingDialogDataEventStream.emitAsync(
                    DebugLoggingDialogData.cantCreateData(
                        logs,
                        userId,
                        customAlertMessage
                    )
                )
            } else {
                debugLoggingDialogDataEventStream.emitAsync(
                    DebugLoggingDialogData.contentReading(
                        true, logs, userId, customAlertMessage
                    )
                )
            }
            return
        }

        val networkResult = safeApiCall {
            nonZoteroApi.debugLogUploadRequest(dataToPost)

        }

        if (networkResult !is CustomResult.GeneralSuccess.NetworkSuccess) {
            debugLoggingDialogDataEventStream.emitAsync(
                DebugLoggingDialogData.upload(logs, userId, customAlertMessage)
            )
            return
        }
        val data = networkResult.value
        if (data == null) {
            debugLoggingDialogDataEventStream.emitAsync(
                DebugLoggingDialogData.responseParsing(logs, userId, customAlertMessage)
            )
            return
        }
        val debugId: String
        try {
            val indexOfReportId = data.indexOf("reportID")
            val indexOfFirstQuotes = data.indexOf("\"", indexOfReportId)
            val indexOfSecondQuotes = data.indexOf("\"", indexOfFirstQuotes + 1)
            debugId = data.substring(indexOfFirstQuotes + 1,indexOfSecondQuotes)
        } catch (e: Exception) {
            Timber.e(e, "DebugLogging: can't upload logs")
            debugLoggingDialogDataEventStream.emitAsync(
                DebugLoggingDialogData.responseParsing(logs, userId, customAlertMessage)
            )
            return
        }
        Timber.i("DebugLogging: uploaded logs")
        clearDebugDirectory()
        val fullDebugId = "D$debugId"
        val customMessage = customAlertMessage?.let { it(fullDebugId) }
        debugLoggingDialogDataEventStream.emitAsync(
            DebugLoggingDialogData.share(fullDebugId, customMessage, userId)
        )
    }

    private suspend fun shareLogs(
        ignoreEmptyLogs: Boolean,
        userId: Long,
        customAlertMessage: ((String) -> String)?
    ) {
        Timber.i("DebugLogging: sharing logs")
        try {
            val logs = fileStore.debugLoggingDirectory().listFiles().toList()

            if (logs.isEmpty()) {
                if (ignoreEmptyLogs) {
                    clearDebugDirectory()
                    return
                }

                Timber.w("DebugLogging: no logs found")
//                throw Error.noLogsRecorded
                return
            }
            submit(logs = logs, userId = userId, customAlertMessage = customAlertMessage)
        } catch (error: Exception) {
            Timber.e(error, "DebugLogging: can't read debug directory contents")
            if (error is Error.cantCreateData) {
                debugLoggingDialogDataEventStream.emitAsync(
                    DebugLoggingDialogData.cantCreateData(emptyList(), userId, customAlertMessage)
                )
            } else {
                debugLoggingDialogDataEventStream.emitAsync(
                    DebugLoggingDialogData.contentReading(
                        false,
                        emptyList(),
                        userId,
                        customAlertMessage
                    )
                )
            }
        }
    }
}
