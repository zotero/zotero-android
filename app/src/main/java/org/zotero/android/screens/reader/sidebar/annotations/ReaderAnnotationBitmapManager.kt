package org.zotero.android.screens.reader.sidebar.annotations

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.screens.reader.sidebar.annotations.cache.ReaderAnnotationBitmapCacheSnapshotEventStream
import org.zotero.android.screens.reader.sidebar.annotations.cache.ReaderAnnotationBitmapMemoryCache
import timber.log.Timber
import javax.inject.Inject

@ViewModelScoped
class ReaderAnnotationBitmapManager @Inject constructor(
    private val dispatchers: Dispatchers,
    private val memoryCache: ReaderAnnotationBitmapMemoryCache,
    private val readerThumbnailPreviewCacheSnapshotEventStream: ReaderAnnotationBitmapCacheSnapshotEventStream,
) {
    private lateinit var viewModelScope: CoroutineScope
    private var coroutineScope: CoroutineScope? = null

    private val batchBitmapsForPostFlow = Channel<Pair<String, Bitmap>>(Channel.UNLIMITED)

    fun init(viewModelScope: CoroutineScope) {
        this.viewModelScope = viewModelScope
        resetManagerState()
    }

    private fun resetManagerState() {
        this.coroutineScope?.cancel()
        this.coroutineScope = CoroutineScope(
            SupervisorJob(viewModelScope.coroutineContext[Job]) +
                    dispatchers.default
        )

        setupBatchBitmapsForPostFlow()
    }

    fun store(key: String, encodedImageBase64String: String) {
        Timber.d("ReaderThumbnailProcessing: store - $key")
        enqueue(
            key = key,
            encodedImageBase64String = encodedImageBase64String,
        )
    }

    private fun enqueue(
        encodedImageBase64String: String,
        key: String,
    ) = coroutineScope?.launch {
        val resultBitmap = convertBase64ToBitmap(encodedImageBase64String)
        batchBitmapsForPostFlow.trySend(key to resultBitmap)
    }

    private fun convertBase64ToBitmap(encodedImageBase64String: String): Bitmap {
        val cleanString = encodedImageBase64String.substringAfter(",")
        val decodedBytes = Base64.decode(cleanString, Base64.DEFAULT)
        val resultBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        return resultBitmap
    }

    private fun convertAndPostResults(
        pageToBitmapList: List<Pair<String, Bitmap>>,
    ) {
        val cacheSnapshot = memoryCache.addToCache(pageToBitmapList)

        val resultCache = generateEmptySnapshot()
        cacheSnapshot.map {
            resultCache[it.key] = it.value
        }

        Timber.d("ReaderThumbnailProcessing: convertAndPostResults: ${pageToBitmapList.map { it.first }}")
        readerThumbnailPreviewCacheSnapshotEventStream.emitAsync(resultCache.toPersistentMap())
    }

    fun generateEmptySnapshot(): MutableMap<String, Bitmap> =
        mutableMapOf()


    private fun setupBatchBitmapsForPostFlow() {
        coroutineScope?.launch {

            val batch = mutableListOf<Pair<String, Bitmap>>()

            while (isActive) {

                // Wait for first item
                val first = batchBitmapsForPostFlow.receive()
                batch += first

                // Collect additional items until timeout
                while (true) {
                    val next = withTimeoutOrNull(200) {
                        batchBitmapsForPostFlow.receive()
                    }

                    if (next == null) {
                        // silence for 300ms -> flush batch
                        break
                    }

                    batch += next
                }
                convertAndPostResults(batch.toList())
                batch.clear()
            }
        }
    }

    fun cancelProcessing() {
        resetManagerState()
        memoryCache.clear()
    }
}