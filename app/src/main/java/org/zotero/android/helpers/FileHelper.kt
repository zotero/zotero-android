package org.zotero.android.helpers

import java.io.File
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.io.encoding.Base64

object FileHelper {
    fun readFileToString(file: File): String {
        return file.readText(Charsets.UTF_8)
    }

    fun deleteFolder(directory: File) {
        if (directory.exists() && directory.isDirectory) {
            directory.deleteRecursively()
        }
    }

    fun toByteArray(str: String): ByteArray {
        return str.toByteArray(Charsets.UTF_8)
    }

    fun toByteArray(stream: InputStream): ByteArray {
        return stream.use { it.readBytes() }
    }

    fun readFileToByteArray(file: File): ByteArray {
        return file.readBytes()
    }

    fun toString(inputStream: InputStream): String {
        return inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
    }

    fun copyInputStreamToFile(inputStream: InputStream, file: File) {
        file.outputStream().use { fileOut ->
            inputStream.copyTo(fileOut)
        }
    }

    fun copyFile(sourceFile: File, targetFile: File) {
        sourceFile.copyTo(targetFile, overwrite = true)
    }

    fun write(file: File, text: String) {
        file.writeText(text, Charsets.UTF_8)
    }

    fun writeByteArrayToFile(file: File, byteArray: ByteArray) {
        file.writeBytes(byteArray)
    }

    fun md5(inputStream: InputStream): String {
        val md5 = DigestInputStream(inputStream, MessageDigest.getInstance("MD5")).use { input ->
            val buffer = ByteArray(1024 * 1024)
            var read = 0
            while (read != -1) {
                read = input.read(buffer)
            }
            input.messageDigest.digest().joinToString("") { "%02x".format(it) }
        }
        return md5
    }

    fun encodeBase64(bytes: ByteArray): ByteArray {
        return Base64.encodeToByteArray(bytes)
    }

    fun decodeBase64(bytes: ByteArray): ByteArray {
        return Base64.decode(bytes)
    }
}