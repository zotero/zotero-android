package org.zotero.android.architecture.logging

import okhttp3.internal.closeQuietly
import org.zotero.android.files.FileStore
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.UUID

class DebugFileWriter constructor(
    private val fileStore: FileStore
) {

    private var fileWriter: FileWriter? = null
    private var bufferWriter: BufferedWriter? = null
    private var printWriter: PrintWriter? = null

    private fun initIfNecessary() {
        if (printWriter != null) {
            return
        }
        val fileName = UUID.randomUUID().toString().replace("-", "")
        val file = File(fileStore.debugLoggingDirectory(), fileName)

        fileWriter = FileWriter(file, true);
        bufferWriter = BufferedWriter(fileWriter);
        printWriter = PrintWriter(bufferWriter!!)
    }

    fun append(stringToAppend: String) {
        initIfNecessary()
        printWriter?.appendLine(stringToAppend + "\n")
    }

    fun flushAndClose() {
        this.printWriter?.flush()

        this.printWriter?.closeQuietly()
        this.bufferWriter?.closeQuietly()
        this.fileWriter?.closeQuietly()

        this.printWriter = null
        this.bufferWriter = null
        this.fileWriter = null
    }
}