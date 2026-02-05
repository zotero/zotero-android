package org.zotero.android.screens.htmlepub.reader.web

import android.content.Context
import android.webkit.WebMessage
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.zotero.android.ZoteroApplication
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.files.FileStore
import org.zotero.android.screens.htmlepub.reader.data.DocumentData
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderWebData
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderWebError
import org.zotero.android.screens.htmlepub.reader.data.Page
import org.zotero.android.translator.data.WebPortResponse
import org.zotero.android.translator.helper.TranslatorHelper.encodeAsJSONForJavascript
import org.zotero.android.translator.helper.TranslatorHelper.encodeStringToBase64Binary
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume

class HtmlEpubReaderWebCallChainExecutor(
    private val context: Context,
    private val dispatchers: Dispatchers,
    private val gson: Gson,
    private val fileStore: FileStore,
    private val webView: WebView,
) {

    private lateinit var htmlEpubReaderWebViewHandler: HtmlEpubReaderWebViewHandler

    private val limitedParallelismDispatcher =
        kotlinx.coroutines.Dispatchers.IO.limitedParallelism(1)
    private var webViewExecutorScope = CoroutineScope(limitedParallelismDispatcher)

    val observable = EventStream<Result<HtmlEpubReaderWebData>>(ZoteroApplication.instance.applicationScope)

    fun start(file: File) {
        try {
            htmlEpubReaderWebViewHandler = HtmlEpubReaderWebViewHandler(
                dispatchers = dispatchers,
                context = context,
                webView = webView,
            )
            initialize(file)
            Timber.i("HtmlEpubReaderWebCallChainExecutor: initialization succeeded")
        } catch (e: Exception) {
            observable.emitAsync(
                Result.Failure(HtmlEpubReaderWebError.failedToInitializeWebView)
            )
            Timber.i(e, "HtmlEpubReaderWebCallChainExecutor: initialization failed")
        }
    }

    private fun initialize(file: File) {
        val filePath = "file://" + file.absolutePath

        loadWebPage(
            url = filePath,
            onWebViewLoadPage = ::onIndexHtmlLoaded,
            processWebViewResponses = ::receiveMessage,
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
                "textHandler" -> {
                    val data = bodyElement.asJsonObject
                    val event = data["event"].asString
                    Timber.i("HtmlEpubReaderWebCallChainExecutor: $event")
                    when (event) {
                        "onInitialized" -> {
                            observable.emitAsync(
                                Result.Success(
                                    HtmlEpubReaderWebData.loadDocument)
                            )
                        }

                        "onSaveAnnotations" -> {
                            val params = data["params"].asJsonObject
                            Timber.i("HtmlEpubReaderWebCallChainExecutor: $params")
                            observable.emitAsync(
                                Result.Success(
                                    HtmlEpubReaderWebData.saveAnnotations(params)
                                )
                            )
                        }

                        "onSetAnnotationPopup" -> {
                            val params = data["params"].asJsonObject

                            if (params.isEmpty) {
                                return@launch
                            }

                            val rectArray = params["rect"].asJsonArray.map { it.asDouble }
                            val key = params["annotation"].asJsonObject["id"].asString


                            //TODO send the event with rect, accommodating for nav and status bar space.
                        }

                        "onSelectAnnotations" -> {
                            val params = data["params"].asJsonObject
                            val ids = params["ids"].asJsonArray.map { it.asString }
                            val key = ids.firstOrNull()
                            if (key != null) {
                                observable.emitAsync(
                                    Result.Success(
                                        HtmlEpubReaderWebData.selectAnnotationFromDocument(key)
                                    )
                                )
                            } else {
                                observable.emitAsync(
                                    Result.Success(
                                        HtmlEpubReaderWebData.deselectSelectedAnnotation
                                    )
                                )
                            }
                        }
                        "onSetSelectionPopup" -> {
                            val params = data["params"].asJsonObject
                            observable.emitAsync(
                                Result.Success(
                                    HtmlEpubReaderWebData.setSelectedTextParams(params)
                                )
                            )
                        }
                        "onChangeViewState" -> {
                            val params = data["params"].asJsonObject
                            observable.emitAsync(
                                Result.Success(
                                    HtmlEpubReaderWebData.setViewState(params)
                                )
                            )
                        }
                        "onOpenLink" -> {
                            val params = data["params"].asJsonObject
                            val url = params["url"].asString
                            observable.emitAsync(
                                Result.Success(
                                    HtmlEpubReaderWebData.showUrl(url)
                                )
                            )
                        }
                        "onSetOutline" -> {
                            observable.emitAsync(
                                Result.Success(
                                    HtmlEpubReaderWebData.parseOutline(data)
                                )
                            )
                        }
                        "onFindResult" -> {
                            observable.emitAsync(
                                Result.Success(
                                    HtmlEpubReaderWebData.processDocumentSearchResults(data)
                                )
                            )
                        }
                        "onBackdropTap" -> {
                            observable.emitAsync(
                                Result.Success(
                                    HtmlEpubReaderWebData.toggleInterfaceVisibility
                                )
                            )
                        }
                    }
                }

                "logHandler" -> {
                    Timber.d("JSLOG: ${bodyElement.asString}")
                }

            }
        }
    }

    private fun onIndexHtmlLoaded() {
        observable.emitAsync(
            Result.Success(
                HtmlEpubReaderWebData.loadDocument)
        )
    }

    suspend fun show(location: Map<String, Any>) {
        val encodedPayload = encodeAsJSONForJavascript(gson = gson, data = location)

        return suspendCancellableCoroutine { cont ->
            htmlEpubReaderWebViewHandler.evaluateJavascript("javascript:navigate({ location: '${encodedPayload}' });") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun selectSearchResult(index: Int) {
        return suspendCancellableCoroutine { cont ->
            htmlEpubReaderWebViewHandler.evaluateJavascript("javascript:window._view.find({ index: '${index}' });") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun search(term: String) {
        val encodedPayload = encodeStringToBase64Binary(term)

        return suspendCancellableCoroutine { cont ->
            htmlEpubReaderWebViewHandler.evaluateJavascript("search({ term: '${encodedPayload}' });") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun clearSearch() {
        return suspendCancellableCoroutine { cont ->
            htmlEpubReaderWebViewHandler.evaluateJavascript("javascript:window._view.find();") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun deselectText() {
        return suspendCancellableCoroutine { cont ->
            htmlEpubReaderWebViewHandler.evaluateJavascript("javascript:window._view.selectAnnotations([]);") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun updateView(modifications: JsonArray, insertions: JsonArray, deletions: JsonArray) {
        val encodedDeletions = encodeAsJSONForJavascript(gson = this.gson, data = deletions)
        val encodedInsertions = encodeAsJSONForJavascript(gson = this.gson, data = insertions)
        val encodedModifications = encodeAsJSONForJavascript(gson = this.gson, data = modifications)

        return suspendCancellableCoroutine { cont ->
            htmlEpubReaderWebViewHandler.evaluateJavascript("javascript:updateAnnotations({ deletions: '${encodedDeletions}', insertions: '${encodedInsertions}', modifications: ${encodedModifications}'});") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun loadDocument(data: DocumentData) {
        Timber.i("HtmlEpubReaderViewModel: try creating view for ${data.type}; page = ${data.page}")
        Timber.i("${data.file.absolutePath}")
        var javascript = "javascript:createView({ type: '${data.type}', url: 'file://${data.file.absolutePath.replace("'", "\'")}', annotations: '${data.annotationsJson}'"
        val key = data.selectedAnnotationKey
        val page = data.page
        if (key != null) {
            javascript += ", location: {annotationID: '$key'}"
        } else if (page != null){
            when(page) {
                is Page.html -> {
                    javascript += ", viewState: {scrollYPercent: ${page.scrollYPercent}, scale: 1}"
                }
                is Page.epub -> {
                    javascript += ", viewState: {cfi: '${page.cfi}'}"
                }
            }
        }
        javascript += "});"
        println()
        return suspendCancellableCoroutine { cont ->
            htmlEpubReaderWebViewHandler.evaluateJavascript(javascript) {
                cont.resume(Unit)
            }
        }

    }


    private fun loadWebPage(
        url: String,
        onWebViewLoadPage: () -> Unit,
        processWebViewResponses: (message: WebMessage) -> Unit,
    ) {
        htmlEpubReaderWebViewHandler.load(
            url = url,
            onWebViewLoadPage = onWebViewLoadPage,
            processWebViewResponses = processWebViewResponses,
        )
    }
}