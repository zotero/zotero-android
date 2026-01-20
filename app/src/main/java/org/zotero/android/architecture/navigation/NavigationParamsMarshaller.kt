package org.zotero.android.architecture.navigation

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.zotero.android.helpers.FileHelper
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationParamsMarshaller @Inject constructor(
    val gson: Gson,
    private val dispatcher: CoroutineDispatcher,
) {

    suspend fun encodeObjectToBase64Async(data: Any, charset: Charset = StandardCharsets.US_ASCII) =
        withContext(dispatcher) {
            encodeObjectToBase64(data = data, charset = charset)
        }

    fun encodeObjectToBase64(data: Any, charset: Charset = StandardCharsets.US_ASCII): String {
        val json = gson.toJson(data)
        val encodedJson = encodeJsonToBase64(stringToEncode = json, charset = charset)
        val escaped = encodedJson.replace('/', '+').replace('_', '-')
        return escaped
    }

    private fun encodeJsonToBase64(
        stringToEncode: String,
        charset: Charset
    ): String {
        val bytes = FileHelper.toByteArray(stringToEncode)
        val encoded: ByteArray = FileHelper.encodeBase64(bytes)
        return String(encoded, charset)
    }

    inline fun <reified T> decodeObjectFromBase64(
        encodedJson: String,
        charset: Charset = StandardCharsets.US_ASCII
    ): T {
        val unescaped = encodedJson.replace('-', '_').replace('+', '/')
        val decodedJson = decodeJsonFromBase64Binary(encodedJson = unescaped, charset = charset)
        return unmarshal(decodedJson)
    }

    inline fun <reified T> unmarshal(data: String): T {
        return gson.fromJson(data, T::class.java)
    }

    fun decodeJsonFromBase64Binary(
        encodedJson: String,
        charset: Charset
    ): String {
        val bytes = FileHelper.toByteArray(encodedJson)
        val decodedJson: ByteArray = FileHelper.decodeBase64(bytes)
        return String(decodedJson, charset)
    }

}