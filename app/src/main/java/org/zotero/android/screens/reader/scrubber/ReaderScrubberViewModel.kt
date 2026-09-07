package org.zotero.android.screens.reader.scrubber

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
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
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.screens.reader.sidebar.data.ReaderScrollReaderIfNeededEvent
import org.zotero.android.screens.reader.sidebar.thumbnails.ReaderThumbnailPreviewManager
import org.zotero.android.screens.reader.sidebar.thumbnails.cache.ReaderThumbnailPreviewCacheSnapshotEventStream
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ReaderScrubberViewModel @Inject constructor(
    private val thumbnailPreviewCacheUpdatedEventStream: ReaderThumbnailPreviewCacheSnapshotEventStream,
    private val thumbnailPreviewManager: ReaderThumbnailPreviewManager,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
) : BaseViewModel2<ReaderScrubberViewState, ReaderScrubberViewEffect>(ReaderScrubberViewState()) {

    private var pdfReaderThemeCancellable: Job? = null
    private var lastRequestedLandmarks: List<Int> = emptyList()
    private var pageLabelHideJob: Job? = null

    companion object {
        private const val PAGE_LABEL_VISIBLE_DURATION_MS = 3000L
    }

    fun initOnce() = initOnce {
        startObservingTheme()
        setupThumbnailCacheUpdateStream()
        thumbnailPreviewManager.init(viewModelScope)
    }

    private fun setupThumbnailCacheUpdateStream() {
        thumbnailPreviewCacheUpdatedEventStream.flow()
            .onEach { cacheSnapshot ->
                Timber.d("ReaderScrubberProcessing: thumbnailCache updated")
                updateState {
                    copy(thumbnailCache = cacheSnapshot)
                }
            }
            .launchIn(viewModelScope)
    }

    private var ignoreChangeByReaderUntil: Long = 0L

    fun onLandmarksComputed(indices: List<Int>) {
        if (indices == lastRequestedLandmarks) {
            return
        }
        lastRequestedLandmarks = indices
        thumbnailPreviewManager.requestExactThumbnails(indices)
    }

    fun onScrubStart() {
        updateState { copy(isScrubbing = true) }
    }

    fun onScrubTo(page: Int) {
        ignoreChangeByReaderUntil = System.currentTimeMillis() + 1000
        if (viewState.selectedPage != page) {
            updateState { copy(selectedPage = page) }
            val location = mapOf("pageNumber" to (page + 1).toString())
            EventBus.getDefault().post(ReaderScrollReaderIfNeededEvent(location))
        }
        thumbnailPreviewManager.requestThumbnail(page)
        showPageLabelTemporarily()
    }

    fun onScrubEnd() {
        updateState { copy(isScrubbing = false) }
    }

    fun onTapAt(page: Int) {
        onScrubTo(page)
    }

    fun onPageChangedByReader(page: Int) {
        val currentTimeMillis = System.currentTimeMillis()
        if (viewState.selectedPage == page || currentTimeMillis < ignoreChangeByReaderUntil) {
            return
        }
        updateState {
            copy(selectedPage = page)
        }
        showPageLabelTemporarily()
    }

    private fun showPageLabelTemporarily() {
        updateState { copy(showPageLabel = true) }
        pageLabelHideJob?.cancel()
        pageLabelHideJob = viewModelScope.launch {
            delay(PAGE_LABEL_VISIBLE_DURATION_MS)
            updateState { copy(showPageLabel = false) }
        }
    }

    private fun clearThumbnailCaches() {
        thumbnailPreviewManager.cancelProcessing()
        lastRequestedLandmarks = emptyList()
    }

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .drop(1)
            .onEach { data ->
                clearThumbnailCaches()
            }
            .launchIn(viewModelScope)
    }
}

internal data class ReaderScrubberViewState(
    val thumbnailCache: ImmutableList<Bitmap?> = persistentListOf(),
    val selectedPage: Int? = null,
    val isScrubbing: Boolean = false,
    val showPageLabel: Boolean = false,
) : ViewState

internal sealed class ReaderScrubberViewEffect : ViewEffect
