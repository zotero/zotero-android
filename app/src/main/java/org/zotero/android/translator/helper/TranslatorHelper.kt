package org.zotero.android.translator.helper

import com.google.gson.Gson
import org.zotero.android.helpers.FileHelper
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets

object TranslatorHelper {

    fun encodeFileToBase64Binary(file: File): String {
        val bytes = FileHelper.readFileToByteArray(file)
        val encoded: ByteArray = FileHelper.encodeBase64(bytes)
        return String(encoded, StandardCharsets.US_ASCII)
    }

    fun encodeFileToBase64Binary(inputStream: InputStream): String {
        val bytes = FileHelper.toByteArray(inputStream)
        val encoded: ByteArray = FileHelper.encodeBase64(bytes)
        return String(encoded, StandardCharsets.US_ASCII)
    }

    fun encodeStringToBase64Binary(stringToEncode: String): String {
        val bytes = FileHelper.toByteArray(stringToEncode)
        val encoded: ByteArray = FileHelper.encodeBase64(bytes)
        return String(encoded, StandardCharsets.US_ASCII)
    }

    fun encodeAsJSONForJavascript(gson: Gson, data: Any): String {
        val json = gson.toJson(data)
        val encodedJson = encodeStringToBase64Binary(json)
        return encodedJson
    }
}