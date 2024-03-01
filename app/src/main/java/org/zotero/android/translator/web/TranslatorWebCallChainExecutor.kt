package org.zotero.android.translator.web

import android.content.Context
import android.webkit.WebMessage
import org.zotero.android.translator.data.WebPortResponse
import org.zotero.android.translator.helper.TranslatorHelper
import org.zotero.android.translator.loader.TranslatorsLoader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.zotero.android.architecture.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume

class TranslatorWebCallChainExecutor @Inject constructor(
    private val context: Context,
    dispatchers: Dispatchers,
    private val gson: Gson,
    private val translatorWebViewHandler: TranslatorWebViewHandler,
    private val translatorsLoader: TranslatorsLoader,
) {
    private val ioCoroutineScope = CoroutineScope(dispatchers.io)

    private lateinit var url: String
    private lateinit var html: String
    private lateinit var frames: List<String>

    fun translate(
        url: String,
        html: String,
        cookies: String,
        frames: List<String>,
        userAgent: String,
        referrer: String
    ) {
        translatorWebViewHandler.set(cookies = cookies, userAgent = userAgent, referrer = referrer)
        this.url = url
        this.html = html
        this.frames = frames
        loadWebPage(
            url = "file:///android_asset/translator/index.html",
            onWebViewLoadPage = ::onTranslatorIndexHtmlLoaded,
            processWebViewResponses = ::receiveMessage
        )
    }

    private fun onTranslatorIndexHtmlLoaded() {
        ioCoroutineScope.launch {
            val loadBundleResult = loadBundleFiles()
            sendInitSchemaAndDateFormatsMessage(loadBundleResult.first, loadBundleResult.second)
            val translatorsResult =
                translatorsLoader.translators(this@TranslatorWebCallChainExecutor.url)
            sendInitTranslatorsMessage(translatorsResult)
            sendTranslateMessage()
        }
    }

    private fun receiveMessage(message: WebMessage) {
        ioCoroutineScope.launch {
            val data = message.data

            val mapType = object : TypeToken<WebPortResponse>() {}.type
            val decodedBody: WebPortResponse = gson.fromJson(data, mapType)
            val handlerName = decodedBody.handlerName
            when (handlerName) {
                "requestHandler" -> {
                    val bodyElement = decodedBody.message
                    if (!bodyElement.isJsonObject || !bodyElement.asJsonObject.has("messageId")) {
                        Timber.e("TranslationWebViewHandler: request missing body - $bodyElement")
                        return@launch
                    }
                    val body = bodyElement.asJsonObject
                    val messageId = body["messageId"].asLong

                    if (!body.has("payload") || !body["payload"].isJsonObject) {
                        Timber.e("TranslationWebViewHandler: request missing payload - $body")
                        translatorWebViewHandler.sendMessaging(
                            error = "HTTP request missing payload",
                            messageId = messageId
                        )
                        return@launch
                    }
                    val options = body["payload"].asJsonObject
                    try {
                        translatorWebViewHandler.sendRequest(
                            options = options,
                            messageId = messageId
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "TranslationWebViewHandler: send request error")
                        //TODO display noSuccessfulTranslators error
                    }
                }

                "itemResponseHandler" -> {
                    val itemResponseMessage = decodedBody.message
                    println(itemResponseMessage)
                }
            }
        }

    }

    private fun loadBundleFiles(): Pair<String, String> {
        val encodedSchemaData =
            TranslatorHelper.encodeFileToBase64Binary(context.assets.open("schema.json"))
        val encodedDateFormatData =
            TranslatorHelper.encodeFileToBase64Binary(context.assets.open("translator/translate/modules/utilities/resource/dateFormats.json"))
        return encodedSchemaData to encodedDateFormatData
    }

    private suspend fun sendInitSchemaAndDateFormatsMessage(
        encodedSchemaData: String,
        encodedDateFormatData: String
    ) {
        return suspendCancellableCoroutine { cont ->
            translatorWebViewHandler.evaluateJavascript("javascript:initSchemaAndDateFormats('${encodedSchemaData}','${encodedDateFormatData}')") {
                cont.resume(Unit)
            }
        }
    }

    private suspend fun sendInitTranslatorsMessage(translatorsJson: String) {
        return suspendCancellableCoroutine { cont ->
            val encodedTranslators = TranslatorHelper.encodeStringToBase64Binary(translatorsJson)

            translatorWebViewHandler.evaluateJavascript("javascript:initTranslators('${encodedTranslators}')") {
                cont.resume(Unit)
            }
        }
    }

    private suspend fun sendTranslateMessage() {
        return suspendCancellableCoroutine { cont ->
            val htmlContentEncoded = TranslatorHelper.encodeStringToBase64Binary(this.html)
            val encodedFrames =
                TranslatorHelper.encodeStringToBase64Binary(gson.toJson(this.frames))
            translatorWebViewHandler.evaluateJavascript("javascript:translate('${url}', '${htmlContentEncoded}', '${encodedFrames}')") {
                cont.resume(Unit)
            }
        }
    }

    private fun loadWebPage(
        url: String,
        onWebViewLoadPage: () -> Unit,
        processWebViewResponses: (message: WebMessage) -> Unit
    ) {
        translatorWebViewHandler.load(
            url = url,
            onWebViewLoadPage = onWebViewLoadPage,
            processWebViewResponses = processWebViewResponses
        )
    }
}