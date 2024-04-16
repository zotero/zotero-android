package org.zotero.android.translator.helper

import com.google.gson.Gson
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets

object TranslatorHelper {

    fun encodeFileToBase64Binary(file: File): String {
        val bytes = FileUtils.readFileToByteArray(file)
        val encoded: ByteArray = Base64.encodeBase64(bytes)
        return String(encoded, StandardCharsets.US_ASCII)
    }

    fun encodeFileToBase64Binary(inputStream: InputStream): String {
        val bytes = IOUtils.toByteArray(inputStream);
        val encoded: ByteArray = Base64.encodeBase64(bytes)
        return String(encoded, StandardCharsets.US_ASCII)
    }

    fun encodeStringToBase64Binary(stringToEncode: String): String {
        val bytes = IOUtils.toByteArray(stringToEncode);
        val encoded: ByteArray = Base64.encodeBase64(bytes)
        return String(encoded, StandardCharsets.US_ASCII)
    }

    fun encodeAsJSONForJavascript(gson: Gson, data: Any): String {
        val json = gson.toJson(data)
        val encodedJson = encodeStringToBase64Binary(json)
        return encodedJson
    }
}