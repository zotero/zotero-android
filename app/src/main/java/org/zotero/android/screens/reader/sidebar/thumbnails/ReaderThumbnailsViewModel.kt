package org.zotero.android.screens.reader.sidebar.thumbnails

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.screens.reader.data.ReaderWebData
import org.zotero.android.screens.reader.sidebar.data.ReaderScrollReaderIfNeededEvent
import org.zotero.android.screens.reader.sidebar.thumbnails.cache.ReaderThumbnailPreviewCacheSnapshotEventStream
import org.zotero.android.screens.reader.web.ReaderWebCallChainEventStream
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ReaderThumbnailsViewModel @Inject constructor(
    private val thumbnailPreviewCacheUpdatedEventStream: ReaderThumbnailPreviewCacheSnapshotEventStream,
    private val thumbnailPreviewManager: ReaderThumbnailPreviewManager,
    private val webCallChainEventStream: ReaderWebCallChainEventStream,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
) : BaseViewModel2<ReaderThumbnailsViewState, ReaderThumbnailsViewEffect>(ReaderThumbnailsViewState()) {

    private var pdfReaderThemeCancellable: Job? = null

    fun initOnce() = initOnce {
        startObservingTheme()
        setupWebCallChainEventStream()
        setupThumbnailCacheUpdateStream()
        thumbnailPreviewManager.init(viewModelScope)
    }

    fun setupWebCallChainEventStream() {
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
            is ReaderWebData.onSetPageLabels -> {
                onSetPageLabels(successValue.pageLabelsJsonArray)
            }

            else -> {
                //no-op
            }
        }
    }

    private fun setupThumbnailCacheUpdateStream() {
        thumbnailPreviewCacheUpdatedEventStream.flow()
            .onEach { cacheSnapshot ->
                Timber.d("ReaderThumbnailProcessing: thumbnailCache updated")
                updateState {
                    copy(thumbnailCache = cacheSnapshot)
                }
            }
            .launchIn(viewModelScope)
    }

    //When we ask the reader to change page it immediatelly triggers onPageChangedByReader in return, which we don't need.
    private var ignoreChangeByReaderUntil: Long = 0L

    fun selectThumbnail(page: Int) {
        ignoreChangeByReaderUntil = System.currentTimeMillis() + 1000
        updateState {
            copy(selectedThumbnailPage = page)
        }
        val location = mapOf("pageNumber" to (page + 1).toString())
        EventBus.getDefault().post(ReaderScrollReaderIfNeededEvent(location))
    }

    fun onPageChangedByReader(page: Int) {
        viewModelScope.launch {
            val currentTimeMillis = System.currentTimeMillis()
            if (viewState.selectedThumbnailPage == page || currentTimeMillis < ignoreChangeByReaderUntil) {
                return@launch
            }
            updateState {
                copy(selectedThumbnailPage = page)
            }
            delay(200)
            triggerEffect(ReaderThumbnailsViewEffect.ScrollThumbnailListToIndex(page))
        }
    }

    private fun onSetPageLabels(pageLabelsJsonArray: JsonArray) {
        updateState {
            copy(pageLabels = pageLabelsJsonArray.map { it.asString })
        }
    }

    private fun clearThumbnailCaches() {
        thumbnailPreviewManager.cancelProcessing()
    }

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .drop(1)
            .onEach { data ->
                clearThumbnailCaches()
            }
            .launchIn(viewModelScope)
    }

    fun requestThumbnail(centerIndex: Int) {
        thumbnailPreviewManager.requestThumbnail(centerIndex)
    }
}

internal data class ReaderThumbnailsViewState(
    val thumbnailCache: ImmutableList<Bitmap?> = persistentListOf(),
    val selectedThumbnailPage: Int? = null,
    val pageLabels: List<String> = emptyList(),
) : ViewState {
    fun isThumbnailSelected(page: Int): Boolean {
        return this.selectedThumbnailPage == page
    }
}

internal sealed class ReaderThumbnailsViewEffect : ViewEffect {
    object NavigateBack : ReaderThumbnailsViewEffect()
    data class ScrollThumbnailListToIndex(val scrollToIndex: Int): ReaderThumbnailsViewEffect()
}
