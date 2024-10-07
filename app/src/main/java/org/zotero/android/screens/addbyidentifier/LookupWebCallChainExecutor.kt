package org.zotero.android.screens.addbyidentifier

import android.content.Context
import android.webkit.WebMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.zotero.android.ZoteroApplication
import org.zotero.android.api.NonZoteroApi
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.files.FileStore
import org.zotero.android.translator.data.WebPortResponse
import org.zotero.android.translator.helper.TranslatorHelper
import org.zotero.android.translator.loader.TranslatorsLoader
import org.zotero.android.screens.addbyidentifier.data.InitializationResult
import org.zotero.android.screens.addbyidentifier.data.LookupData
import org.zotero.android.screens.addbyidentifier.data.LookupError
import org.zotero.android.screens.addbyidentifier.data.LookupSettings
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume

class LookupWebCallChainExecutor(
    val lookupSettings: LookupSettings,
    private val context: Context,
    dispatchers: Dispatchers,
    private val gson: Gson,
    private val translatorsLoader: TranslatorsLoader,
    private val fileStore: FileStore,
    private val nonZoteroApi: NonZoteroApi,
    private val translatorLoadedEventStream: TranslatorLoadedEventStream,
) {

    private lateinit var lookupWebViewHandler: LookupWebViewHandler

    private val limitedParallelismDispatcher =
        kotlinx.coroutines.Dispatchers.IO.limitedParallelism(1)
    private var webViewExecutorScope = CoroutineScope(limitedParallelismDispatcher)

    private var isLoading: InitializationResult = InitializationResult.inProgress

    val observable = EventStream<Result<LookupData>>(ZoteroApplication.instance.applicationScope)

    init {
        try {
            lookupWebViewHandler = LookupWebViewHandler(
                dispatchers = dispatchers,
                context = context,
                gson = gson,
                nonZoteroApi = nonZoteroApi
            )
            initialize()
            Timber.i("LookupWebCallChainExecutor: initialization succeeded")
            isLoading = InitializationResult.initialized
        } catch (e: Exception) {
            Timber.i(e, "LookupWebCallChainExecutor: initialization failed")
            isLoading = InitializationResult.failed(e)
        }
    }

    private fun initialize(
    ) {
        val file = File(fileStore.translatorDirectory(), "lookup.html")
        val filePath = "file://" + file.absolutePath
        loadWebPage(
            url = filePath,
            onWebViewLoadPage = ::onLookupHtmlLoaded,
            processWebViewResponses = ::receiveMessage
        )
    }

    private fun receiveMessage(message: WebMessage) {
        webViewExecutorScope.launch {
            val data = message.data

            val mapType = object : TypeToken<WebPortResponse>() {}.type
            val decodedBody: WebPortResponse = gson.fromJson(data, mapType)
            val handlerName = decodedBody.handlerName
            val bodyElement = decodedBody.message
            when (handlerName) {
                "lookupFailed" -> {
                    val errorNumber = bodyElement.asString.toIntOrNull() ?: return@launch
                    when (errorNumber) {
                        0 -> {
                            observable.emitAsync(
                                Result.Failure(LookupError.invalidIdentifiers)
                            )
                        }

                        1 -> {
                            observable.emitAsync(
                                Result.Failure(LookupError.noSuccessfulTranslators)
                            )
                        }

                        else -> {
                            observable.emitAsync(
                                Result.Failure(LookupError.lookupFailed)
                            )
                        }
                    }
                }

                "itemsHandler" -> {
                    if (!bodyElement.isJsonObject) {
                        return@launch
                    }
                    val rawData = bodyElement.asJsonObject
                    observable.emitAsync(Result.Success(LookupData.item(rawData)))
                }

                "identifiersHandler" -> {
                    if (!bodyElement.isJsonArray) {
                        return@launch
                    }

                    val rawData = bodyElement.asJsonArray
                    observable.emitAsync(Result.Success(LookupData.identifiers(rawData)))
                }

                "logHandler" -> {
                    Timber.i("JSLOG: ${bodyElement.asString}")
                }

                "requestHandler" -> {
                    if (!bodyElement.isJsonObject || !bodyElement.asJsonObject.has("messageId")) {
                        Timber.e("LookupWebCallChainExecutor: request missing body - $bodyElement")
                        return@launch
                    }
                    val body = bodyElement.asJsonObject
                    val messageId = body["messageId"].asLong

                    if (!body.has("payload") || !body["payload"].isJsonObject) {
                        Timber.e("LookupWebCallChainExecutor: request missing payload - $body")
                        lookupWebViewHandler.sendMessaging(
                            error = "HTTP request missing payload",
                            messageId = messageId
                        )
                        return@launch
                    }
                    val options = body["payload"].asJsonObject
                    try {
                        lookupWebViewHandler.sendRequest(
                            options = options,
                            messageId = messageId
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "LookupWebCallChainExecutor: send request error")
                        lookupWebViewHandler.sendMessaging(
                            error = "Could not create request",
                            messageId = messageId
                        )
                    }
                }
            }
        }
    }

    private fun onLookupHtmlLoaded() {
        webViewExecutorScope.launch {
            val loadBundleResult = loadBundleFiles()
            sendInitSchemaAndDateFormatsMessage(loadBundleResult.first, loadBundleResult.second)
            val translatorsResult =
                translatorsLoader.translators(null)
            sendInitTranslatorsMessage(translatorsResult)
            translatorLoadedEventStream.emitAsync(true)
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
            lookupWebViewHandler.evaluateJavascript("javascript:initSchemaAndDateFormats('${encodedSchemaData}','${encodedDateFormatData}')") {
                cont.resume(Unit)
            }
        }
    }

    private suspend fun sendInitTranslatorsMessage(translatorsJson: String) {
        return suspendCancellableCoroutine { cont ->
            val encodedTranslators = TranslatorHelper.encodeStringToBase64Binary(translatorsJson)

            lookupWebViewHandler.evaluateJavascript("javascript:initTranslators('${encodedTranslators}')") {
                cont.resume(Unit)
            }
        }
    }

    private fun loadWebPage(
        url: String,
        onWebViewLoadPage: () -> Unit,
        processWebViewResponses: (message: WebMessage) -> Unit
    ) {
        lookupWebViewHandler.load(
            url = url,
            onWebViewLoadPage = onWebViewLoadPage,
            processWebViewResponses = processWebViewResponses
        )
    }

    suspend fun lookUp(identifier: String) {
        val s = isLoading
        when (s) {
            is InitializationResult.failed -> {
                observable.emitAsync(
                    Result.Failure(s.error)
                )
            }

            InitializationResult.initialized -> {
                _lookUp(identifier = identifier)
            }

            InitializationResult.inProgress -> {
                //no-op
            }
        }
    }

    private suspend fun _lookUp(identifier: String) {
        return suspendCancellableCoroutine { cont ->
            Timber.i("LookupWebCallChainExecutor: call translate js")

            val encodedIdentifiers = TranslatorHelper.encodeStringToBase64Binary(identifier)
            lookupWebViewHandler.evaluateJavascript("javascript:lookup('${encodedIdentifiers}')") {
                cont.resume(Unit)
            }
        }
    }
}