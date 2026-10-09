package org.zotero.android.speech

import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.zotero.android.speech.data.TextRange
import timber.log.Timber

class LocalSpeechProgressListener(
    private val scope: CoroutineScope,
    private val handler: Handler,
) : UtteranceProgressListener() {

    interface Handler {
        fun utteranceStarted(utteranceId: String)
        fun utteranceRangeStarted(utteranceId: String, range: TextRange)
        fun utteranceFinished(utteranceId: String)
        fun utteranceFailed(utteranceId: String)
    }

    override fun onStart(utteranceId: String?) {
        val id = utteranceId ?: return
        scope.launch { handler.utteranceStarted(id) }
    }

    override fun onDone(utteranceId: String?) {
        val id = utteranceId ?: return
        scope.launch { handler.utteranceFinished(id) }
    }

    @Deprecated("Deprecated in Java")
    override fun onError(utteranceId: String?) {
        onError(utteranceId, -1)
    }

    override fun onError(utteranceId: String?, errorCode: Int) {
        val id = utteranceId ?: return
        Timber.e("LocalSpeechProgressListener: utterance $id failed - $errorCode")
        scope.launch { handler.utteranceFailed(id) }
    }

    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
        val id = utteranceId ?: return
        if (end <= start) {
            return
        }
        scope.launch { handler.utteranceRangeStarted(id, TextRange(location = start, length = end - start)) }
    }
}