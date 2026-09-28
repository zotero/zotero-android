package org.zotero.android.speech.documentworker

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.webkit.WebMessage
import com.google.gson.Gson
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.translator.data.WebPortResponse
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class DocumentWorkerWebCallChainExecutor @Inject constructor(
    private val context: Context,
    private val dispatchers: Dispatchers,
    private val gson: Gson,
    private val fileStore: FileStore,
) {
    sealed class Error : Exception() {
        object cantCreateSourceHash : Error()
        data class loadFailed(val reason: String) : Error()
        data class workFailed(val reason: String) : Error()
    }

    private class PendingWork(
        val workId: String,
        val continuation: CancellableContinuation<ByteArray>,
    )

    private val mutex = Mutex()

    private val limitedParallelismDispatcher = dispatchers.io.limitedParallelism(1)
    private val callbackScope = CoroutineScope(limitedParallelismDispatcher)

    @Volatile
    private var pageLoadContinuation: CancellableContinuation<Unit>? = null

    @Volatile
    private var pendingWork: PendingWork? = null

    suspend fun extractStructuredDocumentText(
        file: File,
        contentType: String,
        password: String? = null,
    ): SDTPack = mutex.withLock {
        val sourceHash = withContext(dispatchers.io) { FileHelper.cachedMD5(file) }
            ?: throw Error.cantCreateSourceHash
        val webViewHandler = DocumentWorkerWebViewHandler(
            dispatchers = dispatchers,
            context = context,
        )
        val startTime = System.currentTimeMillis()
        Timber.i("DocumentWorkerWebCallChainExecutor: started structured document text for ${file.name}")
        try {
            loadHostPage(webViewHandler)
            val packedBytes = getStructuredDocumentText(
                webViewHandler = webViewHandler,
                file = file,
                contentType = contentType,
                password = password,
                sourceHash = sourceHash,
            )
            val pack = SDTPack(packedBytes)
            Timber.i("DocumentWorkerWebCallChainExecutor: finished structured document text for ${file.name}, took ${System.currentTimeMillis() - startTime}ms")
            pack
        } finally {
            webViewHandler.destroy()
        }
    }

    private suspend fun loadHostPage(webViewHandler: DocumentWorkerWebViewHandler) {
        val hostPage = File(fileStore.documentWorkerHostDirectory(), "index.html")
        suspendCancellableCoroutine { continuation ->
            pageLoadContinuation = continuation
            webViewHandler.load(
                url = Uri.fromFile(hostPage).toString(),
                onWebViewLoadPage = ::onHostPageLoaded,
                onWebViewFailure = ::onWebViewFailure,
                processWebViewResponses = ::receiveMessage,
            )
            continuation.invokeOnCancellation { pageLoadContinuation = null }
        }
    }

    private suspend fun getStructuredDocumentText(
        webViewHandler: DocumentWorkerWebViewHandler,
        file: File,
        contentType: String,
        password: String?,
        sourceHash: String,
    ): ByteArray {
        val workId = UUID.randomUUID().toString()
        try {
            return suspendCancellableCoroutine { continuation ->
                pendingWork = PendingWork(workId = workId, continuation = continuation)
                val arguments = listOf(workId, Uri.fromFile(file).toString(), contentType, password, sourceHash)
                    .joinToString(", ") { gson.toJson(it) }
                webViewHandler.evaluateJavascript(
                    "typeof getStructuredDocumentText === 'function' ? (getStructuredDocumentText($arguments), 'started') : 'missing'"
                ) { evaluationResult ->
                    if (evaluationResult != "\"started\"") {
                        failPendingWork("getStructuredDocumentText is not available in the document worker host page")
                    }
                }
            }
        } finally {
            pendingWork = null
        }
    }

    private fun onHostPageLoaded() {
        callbackScope.launch {
            val continuation = pageLoadContinuation ?: return@launch
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
    }

    private fun onWebViewFailure(reason: String) {
        Timber.e("DocumentWorkerWebCallChainExecutor: web view failure - $reason")
        callbackScope.launch {
            val continuation = pageLoadContinuation
            if (continuation != null && continuation.isActive) {
                continuation.resumeWithException(Error.loadFailed(reason))
            }
            failPendingWork(reason)
        }
    }

    private fun failPendingWork(reason: String) {
        callbackScope.launch {
            val continuation = pendingWork?.continuation ?: return@launch
            if (continuation.isActive) {
                continuation.resumeWithException(Error.workFailed(reason))
            }
        }
    }

    private fun receiveMessage(message: WebMessage) {
        callbackScope.launch {
            try {
                val response = gson.fromJson(message.data, WebPortResponse::class.java)
                val body = response.message.asJsonObject
                val workId = body["workId"].asString
                val work = pendingWork?.takeIf { it.workId == workId } ?: return@launch
                when (response.handlerName) {
                    "structuredDocumentTextHandler" -> {
                        val buf = body["structuredDocumentText"].asJsonObject["buf"].asString
                        val packedBytes = Base64.decode(buf, Base64.DEFAULT)
                        if (work.continuation.isActive) {
                            work.continuation.resume(packedBytes)
                        }
                    }

                    "errorHandler" -> {
                        val reason = body["error"].asString
                        Timber.e("DocumentWorkerWebCallChainExecutor: structured document text failed - $reason")
                        if (work.continuation.isActive) {
                            work.continuation.resumeWithException(Error.workFailed(reason))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "DocumentWorkerWebCallChainExecutor: can't process web view message")
                failPendingWork("can't process web view message: ${e.message}")
            }
        }
    }
}