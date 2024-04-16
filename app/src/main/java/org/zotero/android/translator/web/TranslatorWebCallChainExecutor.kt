package org.zotero.android.translator.web

import android.content.Context
import android.webkit.WebMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.files.FileStore
import org.zotero.android.translator.data.TranslationWebViewError
import org.zotero.android.translator.data.TranslatorAction
import org.zotero.android.translator.data.TranslatorActionEventStream
import org.zotero.android.translator.data.WebPortResponse
import org.zotero.android.translator.helper.TranslatorHelper
import org.zotero.android.translator.loader.TranslatorsLoader
import org.zotero.android.uicomponents.Strings
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume

class TranslatorWebCallChainExecutor @Inject constructor(
    private val context: Context,
    dispatchers: Dispatchers,
    private val gson: Gson,
    private val translatorWebViewHandler: TranslatorWebViewHandler,
    private val translatorsLoader: TranslatorsLoader,
    private val translatorActionEventStream: TranslatorActionEventStream,
    private val fileStore: FileStore,
) {
    private val ioCoroutineScope = CoroutineScope(dispatchers.io)

    private lateinit var url: String
    private lateinit var html: String
    private lateinit var frames: List<String>

    private var itemSelectionMessageId: Long? = null

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
            val bodyElement = decodedBody.message
            when (handlerName) {
                "itemSelectionHandler" -> {
                    if (!bodyElement.isJsonObject || !bodyElement.asJsonObject.has("messageId")) {
                        Timber.e("item selection missing body - $bodyElement")
                        return@launch
                    }
                    val body = bodyElement.asJsonObject
                    val messageId = body["messageId"].asLong
                    if (!body.has("payload") || !body["payload"].isJsonArray) {
                        Timber.e("item selection missing payload - $body")
                        translatorWebViewHandler.sendMessaging(
                            error = "Item selection missing payload",
                            messageId = messageId
                        )
                        return@launch
                    }

                    val payload = body["payload"].asJsonArray
                    this@TranslatorWebCallChainExecutor.itemSelectionMessageId = messageId
                    var sortedDictionary: MutableList<Pair<String, String>> = mutableListOf()
                    for (data in payload) {
                        val dataAsArray = data.asJsonArray
                        if (dataAsArray.size() != 2) {
                            continue
                        }
                        sortedDictionary.add(Pair(dataAsArray[0].asString, dataAsArray[1].asString))
                    }
                    translatorActionEventStream.emitAsync(
                        Result.Success(
                            TranslatorAction.selectItem(
                                sortedDictionary
                            )
                        )
                    )
                }
                "requestHandler" -> {
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
                        translatorActionEventStream.emitAsync(Result.Failure(TranslationWebViewError.noSuccessfulTranslators))
                    }
                }

                "itemResponseHandler" -> {
                    if (!bodyElement.isJsonArray) {
                        Timber.e("TranslationWebViewHandler: got incompatible body - $bodyElement")
                        translatorActionEventStream.emitAsync(Result.Failure(TranslationWebViewError.incompatibleItem))
                        return@launch
                    }

                    val info = bodyElement.asJsonArray
                    translatorActionEventStream.emitAsync(
                        Result.Success(
                            TranslatorAction.loadedItems(
                                data = info,
                                cookies = translatorWebViewHandler.cookies,
                                userAgent =  translatorWebViewHandler.userAgent,
                                referrer = translatorWebViewHandler.referrer,
                            )
                        )
                    )
                }

                "translationProgressHandler" -> {
                    val progress = bodyElement.asString
                    if (progress == "item_selection") {
                        translatorActionEventStream.emitAsync(
                            Result.Success(
                                TranslatorAction.reportProgress(
                                    context.getString(Strings.shareext_translation_item_selection)
                                )
                            )
                        )

                    } else if (progress.contains("translating_with_")) {
                        val name = progress.substring(17)
                        translatorActionEventStream.emitAsync(
                            Result.Success(
                                TranslatorAction.reportProgress(
                                    context.getString(
                                        Strings.shareext_translation_translating_with,
                                        name
                                    )
                                )
                            )
                        )
                    } else {
                        translatorActionEventStream.emitAsync(
                            Result.Success(
                                TranslatorAction.reportProgress(
                                    progress
                                )
                            )
                        )
                    }
                }

                "saveAsWebHandler" -> {
                    translatorActionEventStream.emitAsync(Result.Failure(TranslationWebViewError.noSuccessfulTranslators))
                }

                "logHandler" -> {
                    Timber.i("JSLOG: ${bodyElement.asString}")
                }
            }
        }
    }

    private fun loadBundleFiles(): Pair<String, String> {
        val encodedSchemaData =
            TranslatorHelper.encodeFileToBase64Binary(context.assets.open("schema.json"))
        val encodedDateFormatData =
            TranslatorHelper.encodeFileToBase64Binary(
                File(
                    fileStore.translatorDirectory(),
                    "translate/modules/utilities/resource/dateFormats.json"
                )
            )
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