package org.zotero.android.screens.reader.readaloud

import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class ReadAloudAnnotationSession(
    private val scope: CoroutineScope,
    private val send: suspend (JsonObject) -> JsonObject?,
    private val unset: suspend (String) -> Unit,
    private val onStart: () -> Unit,
    private val onEnd: (annotation: JsonObject?, key: String?) -> Unit,
) {
    private var isStarted = false
    private var key: String? = null
    private var annotation: JsonObject? = null
    private var pendingParams: JsonObject? = null
    private var pendingEnd: Boolean? = null
    private var sendJob: Job? = null

    fun set(params: JsonObject) {
        if (pendingEnd != null) return
        if (!isStarted) {
            isStarted = true
            onStart()
        }
        pendingParams = params
        if (sendJob == null) {
            sendJob = scope.launch { drain() }
        }
    }

    fun end(store: Boolean) {
        if (!isStarted || pendingEnd != null) return
        pendingEnd = store
        pendingParams = null
        if (sendJob == null) {
            finish()
        }
    }

    private suspend fun drain() {
        try {
            while (true) {
                val params = pendingParams ?: break
                pendingParams = null
                key?.let { params.addProperty("id", it) }
                val response = try {
                    send(params)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "ReadAloudAnnotationSession: setting annotation failed")
                    null
                }
                val responseKey = response?.get("id")?.takeIf { it.isJsonPrimitive }?.asString
                if (response != null && responseKey != null) {
                    key = responseKey
                    annotation = response
                } else {
                    Timber.w("ReadAloudAnnotationSession: reader didn't create annotation")
                }
            }
        } finally {
            sendJob = null
        }
        if (pendingEnd != null) {
            finish()
        }
    }

    private fun finish() {
        val store = pendingEnd ?: return
        val key = key
        val annotation = annotation
        isStarted = false
        pendingEnd = null
        this.key = null
        this.annotation = null
        if (!store && key != null) {
            scope.launch {
                try {
                    unset(key)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "ReadAloudAnnotationSession: unsetting annotation failed")
                }
            }
        }
        onEnd(if (store) annotation else null, key)
    }
}