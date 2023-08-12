package org.zotero.android.architecture.logging

import android.util.Log
import timber.log.Timber.Tree
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugLoggingTree @Inject constructor() : Tree() {

    private var debugFileWriter: DebugFileWriter? = null
    private var debugLogFormatterInterface: DebugLogFormatterInterface? = null

    private var lastTimestamp: Long? = null

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val fileWriter = debugFileWriter ?: return

        val formattedLog = formatLog(priority, tag, message, t)
        debugLogFormatterInterface?.didFormat(formattedLog)
        fileWriter.append(formattedLog)
    }

    private fun formatTimeDiff(diff: Long): String {
        return String.format("(+%07d)", diff)
    }

    private fun formatLog(priority: Int, tag: String?, message: String, t: Throwable?): String {
        val currentTimeStamp = System.currentTimeMillis()
        val formattedTimeDiff = if (this.lastTimestamp == null) {
            formatTimeDiff(0)
        } else {
            val newDiff = currentTimeStamp - this.lastTimestamp!!
            formatTimeDiff(newDiff)
        }
        this.lastTimestamp = currentTimeStamp

        val level = logLevelString(priority)
        val formattedMessage = "$formattedTimeDiff: $level ${
            if (tag != null) {
                "[$tag] "
            } else {
                ""
            }
        }${message}${
            if (t != null) {
                " exception=${t.localizedMessage}"
            } else {
                ""
            }
        }"
        return formattedMessage
    }

    private fun logLevelString(level: Int): String {
        return when (level) {
            Log.DEBUG ->
                "[DEBUG]"

            Log.WARN ->
                "[WARN]"

            Log.ERROR ->
                "[ERROR]"

            Log.INFO ->
                "[INFO]"

            Log.VERBOSE ->
                "[VERBOSE]"

            else ->
                "[UNKNOWN]"
        }
    }

    fun setDebugFileWriter(debugFileWriter: DebugFileWriter?) {
        this.debugFileWriter = debugFileWriter
    }
    fun setDebugLogFormatterInterface(debugLogFormatterInterface: DebugLogFormatterInterface) {
        this.debugLogFormatterInterface = debugLogFormatterInterface
    }
}