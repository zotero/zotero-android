package org.zotero.android.pdfworker.web

import android.content.Context
import android.webkit.WebMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.zotero.android.BuildConfig
import org.zotero.android.ZoteroApplication
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.files.FileStore
import org.zotero.android.pdfworker.data.PdfWorkerRecognizeError
import org.zotero.android.pdfworker.data.PdfWorkerRecognizedData
import org.zotero.android.translator.data.WebPortResponse
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume

class PdfWorkerWebCallChainExecutor(
    private val context: Context,
    private val dispatchers: Dispatchers,
    private val gson: Gson,
    private val fileStore: FileStore,
) {

    private lateinit var pdfWorkerWebViewHandler: PdfWorkerWebViewHandler

    private val limitedParallelismDispatcher =
        kotlinx.coroutines.Dispatchers.IO.limitedParallelism(1)
    private var webViewExecutorScope = CoroutineScope(limitedParallelismDispatcher)

    val observable = EventStream<Result<PdfWorkerRecognizedData>>(ZoteroApplication.instance.applicationScope)

    private lateinit var pdfFilePath: String
    private lateinit var pdfFileName: String

    fun start(pdfFilePath: String, pdfFileName: String) {
        this.pdfFilePath = pdfFilePath
        this.pdfFileName = pdfFileName
        try {
            pdfWorkerWebViewHandler = PdfWorkerWebViewHandler(
                dispatchers = dispatchers,
                context = context,
            )
            initialize()
            Timber.i("PdfWorkerWebCallChainExecutor: initialization succeeded")
        } catch (e: Exception) {
            observable.emitAsync(
                Result.Failure(PdfWorkerRecognizeError.failedToInitializePdfWorker)
            )
            Timber.i(e, "PdfWorkerWebCallChainExecutor: initialization failed")
        }
    }

    private fun initialize() {
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
                "recognizeStage" -> {
                    val jsonObject = bodyElement.asJsonObject
                    val stageId = jsonObject["stageId"].asString
                    val stageData = jsonObject["stageData"]
                    when (stageId) {
                        "ERROR_RECOGNIZE_DOCUMENT" -> {
                            observable.emitAsync(
                                Result.Failure(PdfWorkerRecognizeError.recognizeFailed(stageData.asString))
                            )
                        }

                        "FINISHED_RECOGNIZE_GOT_ITEM_AND_IDENTIFIER" -> {
                            val jsonObject = stageData.asJsonObject
                            val identifier = jsonObject["identifier"].asString
                            val recognizerData = jsonObject["recognizerData"].asJsonObject
                            observable.emitAsync(
                                Result.Success(
                                    PdfWorkerRecognizedData.itemWithIdentifier(
                                        identifier = identifier,
                                        item = recognizerData
                                    )
                                )
                            )
                        }

                        "FINISHED_RECOGNIZE_NO_IDENTIFIER_USING_FALLBACK_ITEM" -> {
                            val rawData = stageData.asJsonObject
                            observable.emitAsync(
                                Result.Success(
                                    PdfWorkerRecognizedData.fallbackItem(
                                        rawData
                                    )
                                )
                            )
                        }

                        "FINISHED_RECOGNIZE_GOT_NOTHING" -> {
                            observable.emitAsync(Result.Success(PdfWorkerRecognizedData.recognizedDataIsEmpty))
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
        webViewExecutorScope.launch {
            sendRecognizePdf(
                pdfFilePath = pdfFilePath,
                pdfFileName = pdfFileName,
            )
        }
    }

    private suspend fun sendRecognizePdf(
        pdfFilePath: String,
        pdfFileName: String,
    ) {
        return suspendCancellableCoroutine { cont ->
            pdfWorkerWebViewHandler.evaluateJavascript("javascript:recognizePdf(${BuildConfig.DEBUG}, '${pdfFilePath}', '${pdfFileName}')") {
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