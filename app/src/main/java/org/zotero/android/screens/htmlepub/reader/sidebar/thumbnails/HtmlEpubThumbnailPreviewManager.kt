package org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.JsonObject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails.cache.HtmlEpubThumbnailPreviewCacheSnapshotEventStream
import org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails.cache.HtmlEpubThumbnailPreviewMemoryCache
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlEpubThumbnailPreviewManager @Inject constructor(
    private val dispatchers: Dispatchers,
    private val memoryCache: HtmlEpubThumbnailPreviewMemoryCache,
    private val eventStream: HtmlEpubThumbnailPreviewCacheSnapshotEventStream
) {
    private var coroutineScope = CoroutineScope(dispatchers.default)

    private val currentlyProcessingThumbnails = Collections.synchronizedSet(mutableSetOf<Int>())
    private val batchBitmapsForPostFlow = Channel<Pair<Int, Bitmap>>(capacity = Channel.UNLIMITED)
    private var numOfPagesInDocument = 0

    fun init(numOfPages: Int) {
        this.numOfPagesInDocument = numOfPages
        setupBatchBitmapsForPostFlow()
    }

    fun store(thumbnailJsonObject: JsonObject) {
        val pageIndex = thumbnailJsonObject["pageIndex"].asInt
        val encodedImageBase64String = thumbnailJsonObject["image"].asString

        if (currentlyProcessingThumbnails.contains(pageIndex)) {
            return
        }

        enqueue(
            pageIndex = pageIndex,
            encodedImageBase64String = encodedImageBase64String,
        )
    }

    private fun enqueue(
        encodedImageBase64String: String,
        pageIndex: Int,
    ) = coroutineScope.launch {
        currentlyProcessingThumbnails.add(pageIndex)
        val resultBitmap = convertBase64ToBitmap(encodedImageBase64String)
        batchBitmapsForPostFlow.send(pageIndex to resultBitmap)
    }

    private fun convertBase64ToBitmap(encodedImageBase64String: String): Bitmap {
        val cleanString = encodedImageBase64String.substringAfter(",")
        val decodedBytes = Base64.decode(cleanString, Base64.DEFAULT)
        val resultBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        return resultBitmap
    }

    private fun convertAndPostResults(
        pageToBitmapList: List<Pair<Int, Bitmap>>,
    ) {
        val cacheSnapshot = memoryCache.addToCache(pageToBitmapList)

        val resultThumbnailCache = MutableList<Bitmap?>(numOfPagesInDocument) { null }
        cacheSnapshot.map {
            resultThumbnailCache[it.key] = it.value
        }

        pageToBitmapList.forEach {
            currentlyProcessingThumbnails.remove(it.first)
        }

        eventStream.emitAsync(resultThumbnailCache.toImmutableList())
    }

    private fun setupBatchBitmapsForPostFlow() {
        coroutineScope.launch {
            batchBitmapsForPostFlow
                .consumeAsFlow()
                .buffer(Channel.UNLIMITED)
                .debounce(300)
                .collect { _ ->
                    val batch = mutableListOf<Pair<Int, Bitmap>>()
                    while (batchBitmapsForPostFlow.isClosedForSend.not() && batchBitmapsForPostFlow.tryReceive().isSuccess) {
                        batchBitmapsForPostFlow.tryReceive().getOrNull()?.let { batch.add(it) }
                    }
                    if (batch.isNotEmpty()) {
                        convertAndPostResults(batch)
                    }
                }
        }
    }


    fun cancelProcessing() {
        coroutineScope.cancel()
        coroutineScope = CoroutineScope(dispatchers.default)
        currentlyProcessingThumbnails.clear()
        memoryCache.clear()
        eventStream.emitAsync(persistentListOf())
    }
}