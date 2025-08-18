package org.zotero.android.helpers

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


object Zipper {
    private const val BUFFER_SIZE = 8192

    @Throws(IOException::class)
    fun zip(files: List<File>, zipFile: File) {
        val out = ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))
        try {
            val data = ByteArray(BUFFER_SIZE)

            for (file in files) {
                val fi = FileInputStream(file)
                val origin = BufferedInputStream(fi, BUFFER_SIZE)
                try {
                    val fullPath = file.absolutePath
                    val entry = ZipEntry(fullPath.substring(fullPath.lastIndexOf("/") + 1))
                    out.putNextEntry(entry)
                    var count: Int
                    while ((origin.read(data, 0, BUFFER_SIZE).also { count = it }) != -1) {
                        out.write(data, 0, count)
                    }
                } finally {
                    out.flush()
                    origin.close()
                }
            }
        } finally {
            out.close()
        }
    }
}