package org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.JsonObject
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderWebData
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubRequestThumbnailRenderEventStream
import org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails.cache.HtmlEpubThumbnailPreviewCacheSnapshotEventStream
import org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails.cache.HtmlEpubThumbnailPreviewMemoryCache
import org.zotero.android.screens.htmlepub.reader.web.HtmlEpubReaderWebCallChainEventStream
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

@ViewModelScoped
class HtmlEpubThumbnailPreviewManager @Inject constructor(
    private val dispatchers: Dispatchers,
    private val memoryCache: HtmlEpubThumbnailPreviewMemoryCache,
    private val htmlEpubThumbnailPreviewCacheSnapshotEventStream: HtmlEpubThumbnailPreviewCacheSnapshotEventStream,
    private val htmlEpubRequestThumbnailRenderEventStream: HtmlEpubRequestThumbnailRenderEventStream,
    private val webCallChainEventStream: HtmlEpubReaderWebCallChainEventStream

) {
    private lateinit var viewModelScope: CoroutineScope
    private var coroutineScope: CoroutineScope? = null

    private val currentlyProcessingThumbnails = Collections.synchronizedSet(mutableSetOf<Int>())

    private val batchBitmapsForPostFlow = Channel<Pair<Int, Bitmap>>(Channel.UNLIMITED)

    private var numOfPagesInDocument = 0

    private val requestThumbnailDebounceFlow = MutableStateFlow(0)
    private val isReaderInitializedFlow = MutableStateFlow(false)

    fun init(viewModelScope: CoroutineScope) {
        this.viewModelScope = viewModelScope
        setupWebCallChainEventStream()
        resetManagerState()
    }

    fun setupWebCallChainEventStream() {
        webCallChainEventStream.flow()
            .onEach { result ->
                process(result)
            }
            .launchIn(viewModelScope)
    }

    private fun process(result: Result<HtmlEpubReaderWebData>) {
        if (result !is Result.Success) {
            return
        }

        when (val successValue = result.value) {
            is HtmlEpubReaderWebData.onRenderThumbnail -> {
                store(
                    successValue.thumbnailJsonObject
                )
            }

            is HtmlEpubReaderWebData.onInitThumbnails -> {
                initNumOfPages(successValue.thumbnailsJsonArray.size())
            }

            else -> {
                //no-op
            }
        }
    }

    private fun initNumOfPages(numberOfPages: Int) {
        isReaderInitializedFlow.tryEmit(true)
        if (this.numOfPagesInDocument != 0) {
            return
        }
        this.numOfPagesInDocument = numberOfPages
        sendEmptyCacheState()
    }


    private fun resetManagerState() {
        this.coroutineScope?.cancel()
        this.coroutineScope = CoroutineScope(
            SupervisorJob(viewModelScope.coroutineContext[Job]) +
                    dispatchers.default
        )

        setupBatchBitmapsForPostFlow()
        setupRequestThumbnailDebounceFlow()
    }

    private fun setupRequestThumbnailDebounceFlow() {
        this.coroutineScope?.let { scope ->
            requestThumbnailDebounceFlow
                .debounce(150)
                .combine(isReaderInitializedFlow
                ) { centerVisibleItemIndex, isReaderInitialized ->
                    if (isReaderInitialized) {
                        requestThumbnailAfterDebounce(centerVisibleItemIndex)
                    }
                }.launchIn(scope)
        }

    }

    private fun requestThumbnailAfterDebounce(centerVisibleItemIndex: Int) {
        val indicesToRequest = centerVisibleItemIndex.let { centerIndex ->
            val start = (centerIndex - 10).coerceAtLeast(0)
            val end = (centerIndex + 10).coerceAtMost(this.numOfPagesInDocument - 1)
            (start..end)
        }.filter { index -> !memoryCache.isInCache(index) }
            .filter { index -> !isCurrentlyProcessing(index) }
        if (indicesToRequest.isNotEmpty()) {
            currentlyProcessingThumbnails.addAll(indicesToRequest)
            Timber.d("HtmlEpubThumbnailProcessing: indicesToRequest = ${indicesToRequest}")
            htmlEpubRequestThumbnailRenderEventStream.emitAsync(indicesToRequest)
        }
    }

    fun requestThumbnail(centerVisibleItemIndex: Int) {
        println()
        coroutineScope?.launch {
            requestThumbnailDebounceFlow.tryEmit(centerVisibleItemIndex)
        }
    }

    fun store(thumbnailJsonObject: JsonObject) {
        val pageIndex = thumbnailJsonObject["pageIndex"].asInt
        val encodedImageBase64String = thumbnailJsonObject["image"].asString

//        if (currentlyProcessingThumbnails.contains(pageIndex)) {
//            Timber.d("HtmlEpubThumbnailProcessing: tried to store already processed: $pageIndex")
//            return
//        }
        Timber.d("HtmlEpubThumbnailProcessing: store - $pageIndex")
        enqueue(
            pageIndex = pageIndex,
            encodedImageBase64String = encodedImageBase64String,
        )
    }

    private fun enqueue(
        encodedImageBase64String: String,
        pageIndex: Int,
    ) = coroutineScope?.launch {
        val resultBitmap = convertBase64ToBitmap(encodedImageBase64String)
        batchBitmapsForPostFlow.trySend(pageIndex to resultBitmap)
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

        val resultThumbnailCache = generateEmptySnapshot()
        cacheSnapshot.map {
            resultThumbnailCache[it.key] = it.value
        }

        pageToBitmapList.forEach {
            currentlyProcessingThumbnails.remove(it.first)
        }
        Timber.d("HtmlEpubThumbnailProcessing: convertAndPostResults: ${pageToBitmapList.map { it.first }}")
        htmlEpubThumbnailPreviewCacheSnapshotEventStream.emitAsync(resultThumbnailCache.toImmutableList())
    }

    fun generateEmptySnapshot(): MutableList<Bitmap?> =
        MutableList(numOfPagesInDocument) { null }


    private fun setupBatchBitmapsForPostFlow() {
        coroutineScope?.launch {

            val batch = mutableListOf<Pair<Int, Bitmap>>()

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

    fun isCurrentlyProcessing(index: Int): Boolean {
        return currentlyProcessingThumbnails.contains(index)
    }

    fun cancelProcessing() {
        isReaderInitializedFlow.tryEmit(false)
        resetManagerState()
        currentlyProcessingThumbnails.clear()
        memoryCache.clear()
        sendEmptyCacheState()
    }

    private fun sendEmptyCacheState() {
        val thumbnailCache = generateEmptySnapshot().toImmutableList()
        htmlEpubThumbnailPreviewCacheSnapshotEventStream.emitAsync(thumbnailCache)
    }
}