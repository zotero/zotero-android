package org.zotero.android.screens.reader.sidebar.annotations

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.JsonObject
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.screens.reader.data.ReaderWebData
import org.zotero.android.screens.reader.sidebar.annotations.cache.ReaderAnnotationBitmapCacheSnapshotEventStream
import org.zotero.android.screens.reader.sidebar.annotations.cache.ReaderAnnotationBitmapMemoryCache
import org.zotero.android.screens.reader.sidebar.data.ReaderRequestAnnotationImageRenderEventStream
import org.zotero.android.screens.reader.web.ReaderWebCallChainEventStream
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

@ViewModelScoped
class ReaderAnnotationBitmapManager @Inject constructor(
    private val dispatchers: Dispatchers,
    private val memoryCache: ReaderAnnotationBitmapMemoryCache,
    private val readerAnnotationBitmapCacheSnapshotEventStream: ReaderAnnotationBitmapCacheSnapshotEventStream,
    private val readerRequestAnnotationImageRenderEventStream: ReaderRequestAnnotationImageRenderEventStream,
    private val webCallChainEventStream: ReaderWebCallChainEventStream,
) {
    private lateinit var viewModelScope: CoroutineScope
    private var coroutineScope: CoroutineScope? = null

    private val currentlyProcessingAnnotationImages = Collections.synchronizedSet(mutableSetOf<String>())

    private val batchBitmapsForPostFlow = Channel<Pair<String, Bitmap>>(Channel.UNLIMITED)
    private val requestAnnotationImageFlow = Channel<String>(Channel.UNLIMITED)

    fun init(viewModelScope: CoroutineScope) {
        this.viewModelScope = viewModelScope
        setupWebCallChainEventStream()
        resetManagerState()
    }

    private fun setupWebCallChainEventStream() {
        webCallChainEventStream.flow()
            .onEach { result ->
                process(result)
            }
            .launchIn(viewModelScope)
    }

    private fun process(result: Result<ReaderWebData>) {
        if (result !is Result.Success) {
            return
        }

        when (val successValue = result.value) {
            is ReaderWebData.onRenderAnnotationImage -> {
                store(successValue.annotationImageJsonObject)
            }

            else -> {
                //no-op
            }
        }
    }

    private fun resetManagerState() {
        this.coroutineScope?.cancel()
        this.coroutineScope = CoroutineScope(
            SupervisorJob(viewModelScope.coroutineContext[Job]) +
                    dispatchers.default
        )

        setupBatchBitmapsForPostFlow()
        setupRequestAnnotationImageFlow()
    }

    private fun setupRequestAnnotationImageFlow() {
        coroutineScope?.launch {
            while (isActive) {

                val first = requestAnnotationImageFlow.receive()
                val batch = mutableSetOf(first)

                while (true) {
                    val next = withTimeoutOrNull(50) {
                        requestAnnotationImageFlow.receive()
                    }

                    if (next == null) {
                        break
                    }

                    batch += next
                }
                requestAnnotationImagesAfterDebounce(batch)
            }
        }
    }

    private fun requestAnnotationImagesAfterDebounce(keys: Set<String>) {
        val keysToRequest = keys
            .filter { key -> !memoryCache.isInCache(key) }
            .filter { key -> !isCurrentlyProcessing(key) }
        if (keysToRequest.isNotEmpty()) {
            currentlyProcessingAnnotationImages.addAll(keysToRequest)
            Timber.d("ReaderAnnotationImageProcessing: keysToRequest = $keysToRequest")
            readerRequestAnnotationImageRenderEventStream.emitAsync(keysToRequest)
        }
    }

    fun requestAnnotationImage(key: String) {
        coroutineScope?.launch {
            requestAnnotationImageFlow.trySend(key)
        }
    }

    fun store(annotationImageJsonObject: JsonObject) {
        val key = annotationImageJsonObject["id"].asString
        val encodedImageBase64String = annotationImageJsonObject["image"].asString

        if (encodedImageBase64String.isEmpty()) {
            currentlyProcessingAnnotationImages.remove(key)
            return
        }

        Timber.d("ReaderAnnotationImageProcessing: store - $key")
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
        keyToBitmapList: List<Pair<String, Bitmap>>,
    ) {
        val cacheSnapshot = memoryCache.addToCache(keyToBitmapList)

        keyToBitmapList.forEach {
            currentlyProcessingAnnotationImages.remove(it.first)
        }
        Timber.d("ReaderAnnotationImageProcessing: convertAndPostResults: ${keyToBitmapList.map { it.first }}")
        readerAnnotationBitmapCacheSnapshotEventStream.emitAsync(cacheSnapshot.toPersistentMap())
    }

    private fun setupBatchBitmapsForPostFlow() {
        coroutineScope?.launch {

            val batch = mutableListOf<Pair<String, Bitmap>>()

            while (isActive) {

                // Wait for first item
                val first = batchBitmapsForPostFlow.receive()
                batch += first

                // Collect additional items until timeout
                while (true) {
                    val next = withTimeoutOrNull(100) {
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

    fun isCurrentlyProcessing(key: String): Boolean {
        return currentlyProcessingAnnotationImages.contains(key)
    }

    fun cancelProcessing() {
        resetManagerState()
        currentlyProcessingAnnotationImages.clear()
        memoryCache.clear()
        readerAnnotationBitmapCacheSnapshotEventStream.emitAsync(persistentMapOf())
    }
}