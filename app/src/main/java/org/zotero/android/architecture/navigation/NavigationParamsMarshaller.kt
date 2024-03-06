package org.zotero.android.architecture.navigation

import com.google.gson.Gson
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationParamsMarshaller @Inject constructor(val gson: Gson) {

    fun encodeObjectToBase64(data: Any): String {
        val json = gson.toJson(data)
        val encodedJson = encodeJsonToBase64(json)
        return encodedJson
    }

    private fun encodeJsonToBase64(stringToEncode: String): String {
        val bytes = IOUtils.toByteArray(stringToEncode);
        val encoded: ByteArray = Base64.encodeBase64(bytes)
        return String(encoded, StandardCharsets.US_ASCII)
    }

    inline fun <reified T> decodeObjectFromBase64(encodedJson: String): T {
        val decodedJson = decodeJsonFromBase64Binary(encodedJson)
        return unmarshal(decodedJson)
    }

    inline fun <reified T> unmarshal(data: String): T {
        return gson.fromJson(data, T::class.java)
    }

    fun decodeJsonFromBase64Binary(encodedJson: String): String {
        val bytes = IOUtils.toByteArray(encodedJson);
        val decodedJson: ByteArray = Base64.decodeBase64(bytes)
        return String(decodedJson, StandardCharsets.US_ASCII)
    }

}