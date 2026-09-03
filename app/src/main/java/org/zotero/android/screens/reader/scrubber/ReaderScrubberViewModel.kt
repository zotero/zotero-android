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

    fun selectThumbnail(page: Int) {
        ignoreChangeByReaderUntil = System.currentTimeMillis() + 1000
        updateState {
            copy(selectedPage = page)
        }
        val location = mapOf("pageNumber" to (page + 1).toString())
        EventBus.getDefault().post(ReaderScrollReaderIfNeededEvent(location))
        scheduleRecenterOnIndex(page)
    }

    fun onPageChangedByReader(page: Int) {
        viewModelScope.launch {
            val currentTimeMillis = System.currentTimeMillis()
            if (viewState.selectedPage == page || currentTimeMillis < ignoreChangeByReaderUntil) {
                return@launch
            }
            updateState {
                copy(selectedPage = page)
            }
            scheduleRecenterOnIndex(page)
        }
    }

    private fun scheduleRecenterOnIndex(page: Int) {
        viewModelScope.launch {
            delay(RECENTER_DELAY_MS)
            triggerEffect(ReaderScrubberViewEffect.ScrollScrubberListToIndex(page))
        }
    }

    companion object {
        private const val RECENTER_DELAY_MS = 300L
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

internal data class ReaderScrubberViewState(
    val thumbnailCache: ImmutableList<Bitmap?> = persistentListOf(),
    val selectedPage: Int? = null,
) : ViewState {
    fun isThumbnailSelected(page: Int): Boolean {
        return this.selectedPage == page
    }
}

internal sealed class ReaderScrubberViewEffect : ViewEffect {
    data class ScrollScrubberListToIndex(val scrollToIndex: Int) : ReaderScrubberViewEffect()
}
