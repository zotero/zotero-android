package org.zotero.android.pdfworker.web

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
import org.zotero.android.pdfworker.data.PdfWorkerRecognizedData
import org.zotero.android.screens.addbyidentifier.data.InitializationResult
import org.zotero.android.translator.data.WebPortResponse
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume

class PdfWorkerWebCallChainExecutor(
    private val context: Context,
    dispatchers: Dispatchers,
    private val gson: Gson,
    private val fileStore: FileStore,
    private val nonZoteroApi: NonZoteroApi,
) {

    private lateinit var pdfWorkerWebViewHandler: PdfWorkerWebViewHandler

    private val limitedParallelismDispatcher =
        kotlinx.coroutines.Dispatchers.IO.limitedParallelism(1)
    private var webViewExecutorScope = CoroutineScope(limitedParallelismDispatcher)

    private var isLoading: InitializationResult = InitializationResult.inProgress

    val observable = EventStream<Result<PdfWorkerRecognizedData>>(ZoteroApplication.instance.applicationScope)

    init {
        try {
            pdfWorkerWebViewHandler = PdfWorkerWebViewHandler(
                dispatchers = dispatchers,
                context = context,
                gson = gson,
                nonZoteroApi = nonZoteroApi
            )
            initialize()
            Timber.i("PdfWorkerWebCallChainExecutor: initialization succeeded")
            isLoading = InitializationResult.initialized
        } catch (e: Exception) {
            Timber.i(e, "PdfWorkerWebCallChainExecutor: initialization failed")
            isLoading = InitializationResult.failed(e)
        }
    }

    private fun initialize(
    ) {
        val file = File(fileStore.pdfWorkerDirectory(), "index.html")
        val filePath = "file://" + file.absolutePath
        loadWebPage(
            url = filePath,
            onWebViewLoadPage = ::onIndexHtmlLoaded,
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
                "recognizePdfData" -> {
                    val rawData = bodyElement
                    Timber.d("PdfWorkerWebCallChainExecutor: recognizePdfData" + rawData)
//                    observable.emitAsync(Result.Success(PdfWorkerRecognizedData.recognizePdfData(rawData)))
                }

                "logHandler" -> {
                    Timber.d("JSLOG: ${bodyElement.asString}")
                }

            }
        }
    }

    private fun onIndexHtmlLoaded() {
        webViewExecutorScope.launch {
            val pdfFilePath = "file:///data/data/org.zotero.android.debug/files/pdf-worker/sample.pdf"

            sendRecognizePdf(
                pdfFilePath = pdfFilePath,
            )
        }
    }

    private suspend fun sendRecognizePdf(
        pdfFilePath: String,
    ) {
        return suspendCancellableCoroutine { cont ->
            pdfWorkerWebViewHandler.evaluateJavascript("javascript:recognizePdf('${pdfFilePath}')") {
                cont.resume(Unit)
            }
        }
    }

    private fun loadWebPage(
        url: String,
        onWebViewLoadPage: () -> Unit,
        processWebViewResponses: (message: WebMessage) -> Unit
    ) {
        pdfWorkerWebViewHandler.load(
            url = url,
            onWebViewLoadPage = onWebViewLoadPage,
            processWebViewResponses = processWebViewResponses
        )
    }
}