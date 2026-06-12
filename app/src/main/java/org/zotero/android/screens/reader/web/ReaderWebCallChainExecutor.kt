package org.zotero.android.screens.reader.web

import android.webkit.WebMessage
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.screens.reader.data.ReaderDocumentData
import org.zotero.android.screens.reader.data.ReaderPage
import org.zotero.android.screens.reader.data.ReaderWebData
import org.zotero.android.screens.reader.data.ReaderWebError
import org.zotero.android.screens.reader.settings.data.PageLayoutFlowMode
import org.zotero.android.screens.reader.settings.data.PageSpreadsMode
import org.zotero.android.screens.reader.web.data.CreateReaderLocation
import org.zotero.android.screens.reader.web.data.CreateReaderViewOptions
import org.zotero.android.screens.reader.web.data.CreateReaderViewState
import org.zotero.android.translator.data.WebPortResponse
import org.zotero.android.translator.helper.TranslatorHelper.encodeAsJSONForJavascript
import org.zotero.android.translator.helper.TranslatorHelper.encodeStringToBase64Binary
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume

@ViewModelScoped
class ReaderWebCallChainExecutor @Inject constructor(
    dispatchers: Dispatchers,
    private val gson: Gson,
    private val observable: ReaderWebCallChainEventStream,
    private val readerWebViewHandler: ReaderWebViewHandler
) {

    private val limitedParallelismDispatcher =
        dispatchers.io.limitedParallelism(1)
    private var webViewExecutorScope = CoroutineScope(limitedParallelismDispatcher)

    fun start(webView: WebView, file: File) {
        try {
            val filePath = "file://" + file.absolutePath

            readerWebViewHandler.load(
                webView = webView,
                url = filePath,
                onWebViewLoadPage = ::onIndexHtmlLoaded,
                processWebViewResponses = ::receiveMessage,
            )

            Timber.i("ReaderWebCallChainExecutor: initialization succeeded")
        } catch (e: Exception) {
            observable.emitAsync(
                Result.Failure(ReaderWebError.failedToInitializeWebView)
            )
            Timber.i(e, "ReaderWebCallChainExecutor: initialization failed")
        }
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
                    Timber.i("ReaderWebCallChainExecutor: $event")
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
                                    ReaderWebData.onInitThumbnails(thumbnailsJsonArray)
                                )
                            )
                        }

                        "onRenderThumbnail" -> {
                            val params = data["params"].asJsonObject
                            val thumbnailJsonObject = params["thumbnail"].asJsonObject
                            observable.emitAsync(
                                Result.Success(
                                    ReaderWebData.onRenderThumbnail(thumbnailJsonObject)
                                )
                            )
                        }

                        "onSetPageLabels" -> {
                            val params = data["params"].asJsonObject
                            val pageLabelsJsonArray = params["pageLabels"].asJsonArray
                            observable.emitAsync(
                                Result.Success(
                                    ReaderWebData.onSetPageLabels(pageLabelsJsonArray)
                                )
                            )
                        }

                        "onSaveAnnotations" -> {
                            val params = data["params"].asJsonObject
                            Timber.i("ReaderWebCallChainExecutor: $params")
                            observable.emitAsync(
                                Result.Success(
                                    ReaderWebData.saveAnnotations(params)
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
                                    ReaderWebData.selectAnnotationFromDocument(key)
                                )
                            )

                            //TODO send the event with rect, accommodating for nav and status bar space.
                        }

                        "onSelectAnnotations" -> {
                            val params = data["params"]
                            if (params == null || params.isJsonNull) {
                                Timber.w("ReaderWebCallChainExecutor: event $event missing params")
                                return@launch
                            }
                            val paramsObject = params.asJsonObject
                            val ids = paramsObject["ids"]
                            if (ids == null || ids.isJsonNull) {
                                Timber.w("ReaderWebCallChainExecutor: event $event missing params")
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
                                        ReaderWebData.selectAnnotationFromDocument(key)
                                    )
                                )
                            } else {
                                observable.emitAsync(
                                    Result.Success(
                                        ReaderWebData.deselectSelectedAnnotation
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
                                    ReaderWebData.setSelectedTextParams(params)
                                )
                            )
                        }

                        "onChangeViewState" -> {
                            val params = data["params"].asJsonObject
                            observable.emitAsync(
                                Result.Success(
                                    ReaderWebData.setViewState(params)
                                )
                            )
                        }

                        "onOpenLink" -> {
                            val params = data["params"].asJsonObject
                            val url = params["url"].asString
                            observable.emitAsync(
                                Result.Success(
                                    ReaderWebData.showUrl(url)
                                )
                            )
                        }

                        "onSetOutline" -> {
                            observable.emitAsync(
                                Result.Success(
                                    ReaderWebData.parseOutline(data)
                                )
                            )
                        }

                        "onFindResult" -> {
                            observable.emitAsync(
                                Result.Success(
                                    ReaderWebData.processDocumentSearchResults(data)
                                )
                            )
                        }

                        "onBackdropTap" -> {
                            observable.emitAsync(
                                Result.Success(
                                    ReaderWebData.toggleInterfaceVisibility
                                )
                            )
                        }

                        "onChangeViewStats" -> {
                            val params = data["params"].asJsonObject
                            observable.emitAsync(
                                Result.Success(
                                    ReaderWebData.setViewStats(params)
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
                ReaderWebData.loadDocument
            )
        )
    }

    suspend fun selectInDocument(key: String) {
        return suspendCancellableCoroutine { cont ->
            readerWebViewHandler.evaluateJavascript("select({ key: '$key' });") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun show(location: Map<String, Any>) {
        val encodedPayload = encodeAsJSONForJavascript(gson = gson, data = location)

        return suspendCancellableCoroutine { cont ->
            readerWebViewHandler.evaluateJavascript("javascript:navigate({ location: '${encodedPayload}' });") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun selectSearchResult(index: Int) {
        return suspendCancellableCoroutine { cont ->
            readerWebViewHandler.evaluateJavascript("javascript:window._view.find({ index: '${index}' });") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun setTool(toolName: String, colorHex: String? = null, size: Float? = null) {
        return suspendCancellableCoroutine { cont ->
            val javascript = "setTool({ type: '$toolName', color: '$colorHex',  size: '$size' });"
            readerWebViewHandler.evaluateJavascript(javascript) {
                cont.resume(Unit)
            }
        }
    }

    suspend fun renderThumbnails(index: Int) {
        return suspendCancellableCoroutine { cont ->
            readerWebViewHandler.evaluateJavascript("renderThumbnails([$index])") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun search(term: String) {
        val encodedPayload = encodeStringToBase64Binary(term)

        return suspendCancellableCoroutine { cont ->
            readerWebViewHandler.evaluateJavascript("search({ term: '${encodedPayload}' });") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun clearTool() {
        return suspendCancellableCoroutine { cont ->
            readerWebViewHandler.evaluateJavascript("clearTool();") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun clearSearch() {
        return suspendCancellableCoroutine { cont ->
            readerWebViewHandler.evaluateJavascript("javascript:window._view.find();") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun deselectText() {
        return suspendCancellableCoroutine { cont ->
            readerWebViewHandler.evaluateJavascript("javascript:window._view.selectAnnotations([]);") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun updateView(modifications: JsonArray, insertions: JsonArray, deletions: JsonArray) {
        val encodedDeletions = encodeAsJSONForJavascript(gson = this.gson, data = deletions)
        val encodedInsertions = encodeAsJSONForJavascript(gson = this.gson, data = insertions)
        val encodedModifications = encodeAsJSONForJavascript(gson = this.gson, data = modifications)

        return suspendCancellableCoroutine { cont ->
            readerWebViewHandler.evaluateJavascript("javascript:updateAnnotations({ deletions: '${encodedDeletions}', insertions: '${encodedInsertions}', modifications: '${encodedModifications}'});") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun loadDocument(data: ReaderDocumentData) {
        Timber.i("ReaderWebCallChainExecutor: try creating view for ${data.type}; page = ${data.page}")
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
            when (page) {
                is ReaderPage.html -> {
                    createReaderViewOptions.viewState =
                        CreateReaderViewState(scrollYPercent = page.scrollYPercent, scale = 1.0)
                }

                is ReaderPage.epub -> {
                    createReaderViewOptions.viewState = CreateReaderViewState(cfi = page.cfi)
                }

                is ReaderPage.pdf -> {
                    createReaderViewOptions.viewState =
                        CreateReaderViewState(pageIndex = page.pageIndex)
                }
            }
        }

        val toJson = encodeAsJSONForJavascript(this.gson, createReaderViewOptions)
        val javascript = "javascript:createView('${toJson}');"
        return suspendCancellableCoroutine { cont ->
            readerWebViewHandler.evaluateJavascript(javascript) {
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
            readerWebViewHandler.evaluateJavascript("window._view.setColorScheme('$appearanceString');") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun setFlowMode(flowMode: PageLayoutFlowMode) {
        val flowModeString = when (flowMode) {
            PageLayoutFlowMode.PAGINATED -> {
                "paginated"
            }

            PageLayoutFlowMode.SCROLLED -> {
                "scrolled"
            }
        }

        return suspendCancellableCoroutine { cont ->
            readerWebViewHandler.evaluateJavascript("window._view.setFlowMode('$flowModeString');") {
                cont.resume(Unit)
            }
        }
    }

    suspend fun setSpreadMode(spreadsMode: PageSpreadsMode) {
        val spreadsModeString = when (spreadsMode) {
            PageSpreadsMode.SINGLE -> {
                "single"
            }

            PageSpreadsMode.DOUBLE -> {
                "double"
            }

            PageSpreadsMode.EVEN -> {
                "even"
            }
        }

        return suspendCancellableCoroutine { cont ->
            readerWebViewHandler.evaluateJavascript("window._view.setSpreadMode('$spreadsModeString');") {
                cont.resume(Unit)
            }
        }
    }

}