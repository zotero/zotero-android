package org.zotero.android.speech

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.zotero.android.api.network.CustomResult
import org.zotero.android.speech.data.RemoteAudioSegment
import org.zotero.android.speech.data.TextRange

class RemoteAudioPreloader(
    private val remoteVoicesController: RemoteVoicesController,
    private val scope: CoroutineScope,
    private val preloadAheadCount: Int = 2,
) {
    class DownloadFailed(val error: CustomResult.GeneralError) : Exception()

    var onPendingSegmentReady: ((TextRange, ByteArray) -> Unit)? = null
    var onPendingSegmentFailed: ((Throwable) -> Unit)? = null

    private val cache = mutableMapOf<TextRange, ByteArray>()
    private val loading = mutableSetOf<TextRange>()
    private val jobs = mutableSetOf<Job>()
    private var pendingPlaybackRange: TextRange? = null

    fun takeCached(range: TextRange): ByteArray? = cache.remove(range)

    fun isLoading(range: TextRange): Boolean = loading.contains(range)

    fun waitForPlayback(range: TextRange) {
        pendingPlaybackRange = range
    }

    fun loadForPlayback(
        segment: RemoteAudioSegment,
        voiceId: String,
        onSuccess: (ByteArray) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        loading.add(segment.range)
        launchTracked {
            try {
                val data = download(segment, voiceId)
                loading.remove(segment.range)
                onSuccess(data)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                loading.remove(segment.range)
                onFailure(error)
            }
        }
    }

    fun ensurePreloaded(after: TextRange, voiceId: String, segmentStartingAt: (Int) -> RemoteAudioSegment?) {
        val segmentsToLoad = preloadAheadCount - (cache.size + loading.size)
        if (segmentsToLoad <= 0) {
            return
        }
        var nextIndex = after.end
        var loaded = 0
        while (loaded < segmentsToLoad) {
            val segment = segmentStartingAt(nextIndex) ?: break
            val range = segment.range
            if (range.end <= nextIndex) {
                break
            }
            if (cache.containsKey(range) || loading.contains(range)) {
                nextIndex = range.end
                continue
            }
            loading.add(range)
            preload(segment, voiceId, segmentStartingAt)
            nextIndex = range.end
            loaded += 1
        }
    }

    fun clear() {
        jobs.toList().forEach { it.cancel() }
        jobs.clear()
        cache.clear()
        loading.clear()
        pendingPlaybackRange = null
    }

    private fun preload(segment: RemoteAudioSegment, voiceId: String, segmentStartingAt: (Int) -> RemoteAudioSegment?) {
        val range = segment.range
        launchTracked {
            try {
                val data = download(segment, voiceId)
                loading.remove(range)
                if (pendingPlaybackRange == range) {
                    pendingPlaybackRange = null
                    onPendingSegmentReady?.invoke(range, data)
                } else {
                    cache[range] = data
                }
                ensurePreloaded(range, voiceId, segmentStartingAt)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                loading.remove(range)
                if (pendingPlaybackRange == range) {
                    pendingPlaybackRange = null
                    onPendingSegmentFailed?.invoke(error)
                }
            }
        }
    }

    private suspend fun download(segment: RemoteAudioSegment, voiceId: String): ByteArray {
        val result = remoteVoicesController.downloadSound(text = segment.text, voiceId = voiceId)
        if (result is CustomResult.GeneralError.CodeError) {
            throw result.throwable
        }
        if (result is CustomResult.GeneralError) {
            throw DownloadFailed(result)
        }
        return (result as CustomResult.GeneralSuccess).value ?: throw RemoteVoicesController.Error.noData
    }

    private fun launchTracked(block: suspend () -> Unit) {
        val job = scope.launch { block() }
        jobs.add(job)
        job.invokeOnCompletion { jobs.remove(job) }
    }
}