package org.zotero.android.translator.web

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.translator.data.RawAttachment
import org.zotero.android.translator.data.TranslationWebViewError
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume

class TranslatorWebExtractionExecutor @Inject constructor(
    private val gson: Gson,
    private val translatorWebViewHandler: TranslatorWebViewHandler,
    private val fileStore: FileStore,
) {

    suspend fun execute(url: String): RawAttachment {
        val jsonObject = executeWebViewExtractionJs(url)
        val parsedRawAttachment = parseJsonResponse(url = url, payload = jsonObject)
        return parsedRawAttachment
    }

    private suspend fun executeWebViewExtractionJs(url: String): JsonObject {
        return suspendCancellableCoroutine { cont ->
            translatorWebViewHandler.load(
                url = url,
                onWebViewLoadPage = {
                    val extractionJsContent =
                        FileHelper.readFileToString(
                            File(
                                fileStore.translatorDirectory(),
                                "webview_extraction.js"
                            )
                        )
                    translatorWebViewHandler.evaluateJavascript(extractionJsContent) { javascriptResponse ->
                        val mapType = object : TypeToken<JsonObject>() {}.type
                        val jsonObject: JsonObject = gson.fromJson(javascriptResponse, mapType)
                        cont.resume(jsonObject)
                    }
                },
            )
        }

    }

    private fun parseJsonResponse(url: String, payload: JsonObject): RawAttachment {
        val isFile: Boolean
        val cookies: String
        val userAgent: String
        val referrer: String
        try {
            isFile = payload["isFile"].asBoolean
            cookies = payload["cookies"].asString
            userAgent = payload["userAgent"].asString
            referrer = payload["referrer"].asString
        } catch (e: Exception) {
            Timber.e("WebViewHandler: extracted data missing response")
            Timber.e(payload.toString())
            throw TranslationWebViewError.webExtractionMissingData
        }
        val contentType = payload["contentType"]?.asString
        val title = payload["title"]?.asString
        val html = payload["html"]?.asString
        val frames = payload["frames"]?.asJsonArray?.map { it.asString }

        if (isFile && contentType != null) {
            Timber.i("WebViewHandler: extracted file")
            return RawAttachment.remoteFileUrl(
                url = url,
                contentType = contentType,
                cookies = cookies,
                userAgent = userAgent,
                referrer = referrer
            )
        } else if (title != null && html != null && frames != null) {
            Timber.i("WebViewHandler: extracted html")
            return RawAttachment.web(
                title = title,
                url = url,
                html = html,
                cookies = cookies,
                frames = frames,
                userAgent = userAgent,
                referrer = referrer
            )
        } else {
            Timber.e("WebViewHandler: extracted data incompatible")
            Timber.e(payload.toString())
            throw TranslationWebViewError.webExtractionMissingData
        }
    }
}