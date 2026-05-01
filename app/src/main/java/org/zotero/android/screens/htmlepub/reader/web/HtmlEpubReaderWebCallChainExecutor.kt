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
import org.zotero.android.screens.htmlepub.reader.CreateReaderLocation
import org.zotero.android.screens.htmlepub.reader.CreateReaderViewOptions
import org.zotero.android.screens.htmlepub.reader.CreateReaderViewState
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
                fileStore = fileStore,
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
                            //We rely on WebView's onPageLoaded event instead.
//                            observable.emitAsync(
//                                Result.Success(
//                                    HtmlEpubReaderWebData.loadDocument)
//                            )
                        }

                        "onInitThumbnails" -> {
                            val params = data["params"].asJsonObject
                            val thumbnailsJsonArray = params["thumbnails"].asJsonArray
                            observable.emitAsync(
                                Result.Success(
                                    HtmlEpubReaderWebData.onInitThumbnails(thumbnailsJsonArray)
                                )
                            )
                        }

                        "onRenderThumbnail" -> {
                            val params = data["params"].asJsonObject
                            val thumbnailJsonObject = params["thumbnail"].asJsonObject
                            observable.emitAsync(
                                Result.Success(
                                    HtmlEpubReaderWebData.onRenderThumbnail(thumbnailJsonObject)
                                )
                            )
                        }

                        "onSetPageLabels" -> {
                            val params = data["params"].asJsonObject
                            val pageLabelsJsonArray = params["pageLabels"].asJsonArray
                            observable.emitAsync(
                                Result.Success(
                                    HtmlEpubReaderWebData.onSetPageLabels(pageLabelsJsonArray)
                                )
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

//                            val rectArray = params["rect"].asJsonArray.map { it.asDouble }
                            val key = params["annotation"].asJsonObject["id"].asString
                            observable.emitAsync(
                                Result.Success(
                                    HtmlEpubReaderWebData.selectAnnotationFromDocument(key)
                                )
                            )

                            //TODO send the event with rect, accommodating for nav and status bar space.
                        }

                        "onSelectAnnotations" -> {
                            val params = data["params"]
                            if (params  == null || params.isJsonNull) {
                                Timber.w("HtmlEpubReaderWebCallChainExecutor: event $event missing params")
                                return@launch
                            }
                            val paramsObject = params.asJsonObject
                            val ids = paramsObject["ids"]
                            if (ids == null || ids.isJsonNull) {
                                Timber.w("HtmlEpubReaderWebCallChainExecutor: event $event missing params")
                                return@launch
                            }
                            val idsJsonArrayOfStrings = ids.asJsonArray.mapNotNull {
                                if (it.isJsonNull) {
                                    null
                                } else {
                                    it.asString
                                }
                          }
                            val key = idsJsonArrayOfStrings.firstOrNull()
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
                            val dParams = data["params"]
                            if (dParams == null
                                || dParams.isJsonNull
                                || (dParams.isJsonObject && dParams.asJsonObject.isEmpty)
                            ) {
                                return@launch
                            }
                            val params = dParams.asJsonObject
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

    suspend fun selectInDocument(key: String) {
        return suspendCancellableCoroutine { cont ->
            htmlEpubReaderWebViewHandler.evaluateJavascript("select({ key: '$key' });") {
                cont.resume(Unit)
            }
        }
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

    suspend fun setTool(toolName: String, colorHex: String? = null, size: Float? = null) {
        return suspendCancellableCoroutine { cont ->
            val javascript = "setTool({ type: '$toolName', color: '$colorHex',  size: '$size' });"
            htmlEpubReaderWebViewHandler.evaluateJavascript(javascript) {
                cont.resume(Unit)
            }
        }
    }

    suspend fun renderThumbnails(thumbnailIndices: List<Int>) {
        return suspendCancellableCoroutine { cont ->
            val json = gson.toJson(thumbnailIndices)
            htmlEpubReaderWebViewHandler.evaluateJavascript("renderThumbnails($json)") {
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

    suspend fun clearTool() {
        return suspendCancellableCoroutine { cont ->
            htmlEpubReaderWebViewHandler.evaluateJavascript("clearTool();") {
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
            htmlEpubReaderWebViewHandler.evaluateJavascript("javascript:updateAnnotations({ deletions: '${encodedDeletions}', insertions: '${encodedInsertions}', modifications: '${encodedModifications}'});") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun loadDocument(data: DocumentData) {
        Timber.i("HtmlEpubReaderViewModel: try creating view for ${data.type}; page = ${data.page}")
        Timber.i("${data.file.absolutePath}")
        val createReaderViewOptions = CreateReaderViewOptions(
            type = data.type,
            url = "https://appassets.androidplatform.net/local/${data.file.name}",
            annotations = data.annotationsJson
        )

        val key = data.selectedAnnotationKey
        val page = data.page
        if (key != null) {
            createReaderViewOptions.location = CreateReaderLocation(annotationID = key)
        } else if (page != null) {
            when(page) {
                is Page.html -> {
                    createReaderViewOptions.viewState = CreateReaderViewState(scrollYPercent = page.scrollYPercent, scale = 1.0)
                }
                is Page.epub -> {
                    createReaderViewOptions.viewState = CreateReaderViewState(cfi = page.cfi)
                }
                is Page.pdf -> {
                    createReaderViewOptions.viewState = CreateReaderViewState(pageIndex = page.pageIndex)
                }
            }
        }

        val toJson = encodeAsJSONForJavascript(this.gson, createReaderViewOptions)
        val javascript = "javascript:createView('${toJson}');"
        return suspendCancellableCoroutine { cont ->
            htmlEpubReaderWebViewHandler.evaluateJavascript(javascript) {
                cont.resume(Unit)
            }
        }

    }

    suspend fun updateInterface(isDark: Boolean) {
        val appearanceString = if (isDark) {
            "dark"
        } else {
            "light"
        }

        return suspendCancellableCoroutine { cont ->
            htmlEpubReaderWebViewHandler.evaluateJavascript("window._view.setColorScheme('$appearanceString');") {
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