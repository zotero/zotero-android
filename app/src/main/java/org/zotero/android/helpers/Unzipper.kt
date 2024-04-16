package org.zotero.android.helpers

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

private val BUFFER_SIZE = 8192

@Singleton
class Unzipper @Inject constructor() {
    @Throws(Exception::class)
    fun unzip(zipFile: String, location: String) {
        var location = location
        var size: Int
        val buffer = ByteArray(BUFFER_SIZE)
        if (!location.endsWith(File.separator)) {
            location += File.separator
        }
        val f = File(location)
        if (!f.isDirectory()) {
            f.mkdirs()
        }
        val zin = ZipInputStream(BufferedInputStream(FileInputStream(zipFile), BUFFER_SIZE))
        try {
            var ze: ZipEntry? = null
            while (zin.nextEntry.also { ze = it } != null) {
                val path = location + ze!!.name
                val unzipFile = File(path)
                if (ze!!.isDirectory) {
                    if (!unzipFile.isDirectory()) {
                        unzipFile.mkdirs()
                    }
                } else {
                    // check for and create parent directories if they don't exist
                    val parentDir: File? = unzipFile.getParentFile()
                    if (null != parentDir) {
                        if (!parentDir.isDirectory()) {
                            parentDir.mkdirs()
                        }
                    }

                    // unzip the file
                    val out: FileOutputStream = FileOutputStream(unzipFile, false)
                    val fout = BufferedOutputStream(out, BUFFER_SIZE)
                    try {
                        while (zin.read(buffer, 0, BUFFER_SIZE).also { size = it } != -1) {
                            fout.write(buffer, 0, size)
                        }
                        zin.closeEntry()
                    } finally {
                        fout.flush()
                        fout.close()
                    }
                }
            }
        } finally {
            zin.close()
        }
    }

    @Throws(Exception::class)
    fun unzipStream(zipInputStream: InputStream, location: String) {
        var location = location
        var size: Int
        val buffer = ByteArray(BUFFER_SIZE)
        if (!location.endsWith(File.separator)) {
            location += File.separator
        }
        val f = File(location)
        if (!f.isDirectory()) {
            f.mkdirs()
        }
        val zin = ZipInputStream(BufferedInputStream(zipInputStream, BUFFER_SIZE))
        try {
            var ze: ZipEntry? = null
            while (zin.nextEntry.also { ze = it } != null) {
                val path = location + ze!!.name
                val unzipFile = File(path)
                if (ze!!.isDirectory) {
                    if (!unzipFile.isDirectory()) {
                        unzipFile.mkdirs()
                    }
                } else {
                    // check for and create parent directories if they don't exist
                    val parentDir: File? = unzipFile.getParentFile()
                    if (null != parentDir) {
                        if (!parentDir.isDirectory()) {
                            parentDir.mkdirs()
                        }
                    }

                    // unzip the file
                    val out: FileOutputStream = FileOutputStream(unzipFile, false)
                    val fout = BufferedOutputStream(out, BUFFER_SIZE)
                    try {
                        while (zin.read(buffer, 0, BUFFER_SIZE).also { size = it } != -1) {
                            fout.write(buffer, 0, size)
                        }
                        zin.closeEntry()
                    } finally {
                        fout.flush()
                        fout.close()
                    }
                }
            }
        } finally {
            zin.close()
        }
    }

}