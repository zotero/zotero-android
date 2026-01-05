package org.zotero.android.architecture.logging.crash

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.zotero.android.api.NonZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.logging.DeviceInfoProvider
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashReporter @Inject constructor(
    dispatcher: CoroutineDispatcher,
    private val fileStore: FileStore,
    private val crashShareDataEventStream: CrashShareDataEventStream,
    private val nonZoteroApi: NonZoteroApi,
) {

    sealed class Error : Exception() {
        object responseParsing : Error()
    }

    private var coroutineScope = CoroutineScope(dispatcher)

    fun processPendingReports() {
        coroutineScope.launch {
            val crashFile = getMostRecentFileCrash()
            if (crashFile == null) {
                return@launch
            }
            val text = FileHelper.readFileToString(crashFile)
            val date = crashFile.name.toLongOrNull() ?: return@launch
            val submitResult = submit(text)
            if (submitResult !is CustomResult.GeneralSuccess) {
                Timber.e("CrashReporter: can't upload crash log")
                cleanup()
                return@launch
            }
            val reportId = submitResult.value!!
            reportCrashIfNeeded(id = reportId, date = date)
            cleanup()
        }

    }

    private suspend fun submit(
        crashLog: String,
    ): CustomResult<String?> {
        val networkResult = safeApiCall {
            nonZoteroApi.crashLogUploadRequest(
                errorData = crashLog,
                diagnostic = DeviceInfoProvider.crashString
            )
        }
        if (networkResult !is CustomResult.GeneralSuccess.NetworkSuccess) {
            return networkResult
        }
        val data = networkResult.value
        if (data == null) {
            return CustomResult.GeneralError.CodeError(Error.responseParsing)
        }
        try {
            val indexOfReportId = data.indexOf("reportID")
            val indexOfFirstQuotes = data.indexOf("\"", indexOfReportId)
            val indexOfSecondQuotes = data.indexOf("\"", indexOfFirstQuotes + 1)
            val debugId = data.substring(indexOfFirstQuotes + 1, indexOfSecondQuotes)
            return CustomResult.GeneralSuccess(debugId)
        } catch (e: Exception) {
            Timber.e(e, "CrashReporter: can't parse logs")
            return CustomResult.GeneralError.CodeError(Error.responseParsing)
        }
    }

    private fun reportCrashIfNeeded(id: String, date: Long?) {
        if (date == null) {
            crashShareDataEventStream.emitAsync(CrashReportIdDialogData(id))
            return
        }
        if ((System.currentTimeMillis() - date) >= 1000 * 60 * 10) {
            return
        }
        crashShareDataEventStream.emitAsync(CrashReportIdDialogData(id))
    }

    private fun getMostRecentFileCrash(): File? {
        return fileStore
            .crashLoggingDirectory()
            .listFiles()
            ?.maxByOrNull { it.lastModified() }
    }

    private fun cleanup() {
        try {
            FileHelper.deleteFolder(fileStore.crashLoggingDirectory())
        } catch (error: Exception) {
            Timber.e(error, "CrashLogging: can't delete directory")
        }
    }
}