package org.zotero.android.documentworker.web

import android.content.Context
import android.webkit.WebMessage
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.zotero.android.BuildConfig
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.files.FileStore
import org.zotero.android.pdfworker.web.PdfWorkerWebViewHandler
import org.zotero.android.translator.data.WebPortResponse
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DocumentWorkerWebCallChainExecutor(
    private val context: Context,
    private val dispatchers: Dispatchers,
    private val gson: Gson,
    private val fileStore: FileStore,
) {
    class SDTGenerationException(message: String) : Exception(message)

    private var webViewHandler: PdfWorkerWebViewHandler? = null

    private val limitedParallelismDispatcher =
        kotlinx.coroutines.Dispatchers.IO.limitedParallelism(1)
    private val webViewExecutorScope = CoroutineScope(limitedParallelismDispatcher)

    private var pageLoaded = CompletableDeferred<Unit>()
    private var requestIdCounter = 0

    private var currentRequestId: String? = null
    private var currentContinuation: kotlin.coroutines.Continuation<ByteArray>? = null
    private var currentOnProgress: ((Int) -> Unit)? = null

    fun start() {
        webViewExecutorScope.launch {
            try {
                webViewHandler = PdfWorkerWebViewHandler(
                    dispatchers = dispatchers,
                    context = context,
                )
                val file = File(fileStore.documentWorkerDirectory(), "index.html")
                val filePath = "file://" + file.absolutePath
                webViewHandler?.load(
                    url = filePath,
                    onWebViewLoadPage = ::onIndexHtmlLoaded,
                    processWebViewResponses = ::receiveMessage
                )
                Timber.i("DocumentWorkerWebCallChainExecutor: initialization started")
            } catch (e: Exception) {
                Timber.e(e, "DocumentWorkerWebCallChainExecutor: initialization failed")
                pageLoaded.completeExceptionally(e)
            }
        }
    }

    private fun onIndexHtmlLoaded() {
        webViewHandler?.evaluateJavascript("javascript:initDocumentWorker(${BuildConfig.DEBUG})") {}
    }

    suspend fun generateSDT(
        file: File,
        contentType: String,
        sourceHash: String,
        password: String?,
        onProgress: (Int) -> Unit,
    ): ByteArray {
        pageLoaded.await()
        requestIdCounter++
        val requestId = "sdt_$requestIdCounter"
        return suspendCancellableCoroutine { cont ->
            currentRequestId = requestId
            currentContinuation = cont
            currentOnProgress = onProgress
            val fileUrl = "file://" + file.absolutePath
            val passwordArg = if (password != null) "'${escapeJs(password)}'" else "null"
            webViewHandler?.evaluateJavascript(
                "javascript:generateSDT('${requestId}', '${escapeJs(fileUrl)}', '${
                    escapeJs(
                        contentType
                    )
                }', '${escapeJs(sourceHash)}', $passwordArg)"
            ) {}
        }
    }

    private fun escapeJs(value: String): String {
        return value.replace("\\", "\\\\").replace("'", "\\'")
    }

    private fun receiveMessage(message: WebMessage) {
        webViewExecutorScope.launch {
            val data = message.data
            val mapType = object : TypeToken<WebPortResponse>() {}.type
            val decodedBody: WebPortResponse = gson.fromJson(data, mapType)
            val handlerName = decodedBody.handlerName
            val bodyElement = decodedBody.message
            when (handlerName) {
                "documentWorkerReady" -> {
                    if (!pageLoaded.isCompleted) {
                        pageLoaded.complete(Unit)
                    }
                }

                "documentWorkerError" -> {
                    Timber.e("DocumentWorkerWebCallChainExecutor: worker error: $bodyElement")
                    if (!pageLoaded.isCompleted) {
                        pageLoaded.completeExceptionally(SDTGenerationException(bodyElement.toString()))
                    }
                    failCurrent(
                        bodyElement.asJsonObject["message"]?.asString ?: "Document worker error"
                    )
                }

                "sdtProgress" -> {
                    val jsonObject = bodyElement.asJsonObject
                    val requestId = jsonObject["requestId"].asString
                    val progress = jsonObject["progress"].asInt
                    if (requestId == currentRequestId) {
                        currentOnProgress?.invoke(progress)
                    }
                }

                "sdtResult" -> {
                    val jsonObject = bodyElement.asJsonObject
                    val requestId = jsonObject["requestId"].asString
                    if (requestId == currentRequestId) {
                        val bytesBase64 = jsonObject["bytes"].asString
                        val bytes = Base64.decode(bytesBase64, Base64.DEFAULT)
                        completeCurrent(bytes)
                    }
                }

                "sdtError" -> {
                    val jsonObject = bodyElement.asJsonObject
                    val requestId = jsonObject["requestId"].asString
                    if (requestId == currentRequestId) {
                        val errorMessage =
                            jsonObject["message"]?.asString ?: "Failed to generate SDT"
                        failCurrent(errorMessage)
                    }
                }

                "logHandler" -> {
                    Timber.d("JSLOG(document-worker): $bodyElement")
                }
            }
        }
    }

    private fun completeCurrent(bytes: ByteArray) {
        val cont = currentContinuation ?: return
        currentContinuation = null
        currentRequestId = null
        currentOnProgress = null
        cont.resume(bytes)
    }

    private fun failCurrent(message: String) {
        val cont = currentContinuation ?: return
        currentContinuation = null
        currentRequestId = null
        currentOnProgress = null
        cont.resumeWithException(SDTGenerationException(message))
    }
}