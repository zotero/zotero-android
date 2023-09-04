package org.zotero.android.architecture.logging.crash

import okhttp3.internal.closeQuietly
import org.zotero.android.files.FileStore
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashFileWriter @Inject constructor(
    private val fileStore: FileStore
) {

    private var fileWriter: FileWriter? = null
    private var bufferWriter: BufferedWriter? = null
    private var printWriter: PrintWriter? = null

    fun writeCrashToFile(stackTrace: String) {
        val fileName = System.currentTimeMillis().toString()
        val file = File(fileStore.crashLoggingDirectory(), fileName)

        fileWriter = FileWriter(file, false);
        bufferWriter = BufferedWriter(fileWriter);
        printWriter = PrintWriter(bufferWriter!!)
        printWriter?.write(stackTrace)
        flushAndClose()
    }

    private fun flushAndClose() {
        this.printWriter?.flush()

        this.printWriter?.closeQuietly()
        this.bufferWriter?.closeQuietly()
        this.fileWriter?.closeQuietly()

        this.printWriter = null
        this.bufferWriter = null
        this.fileWriter = null
    }

}