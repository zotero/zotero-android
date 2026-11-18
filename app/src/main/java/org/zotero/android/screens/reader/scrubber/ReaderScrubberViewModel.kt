package org.zotero.android.screens.reader.scrubber

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.screens.reader.sidebar.data.ReaderHistoryTrackingEvent
import org.zotero.android.screens.reader.sidebar.data.ReaderScrollReaderIfNeededEvent
import org.zotero.android.screens.reader.sidebar.thumbnails.ReaderThumbnailPreviewManager
import org.zotero.android.screens.reader.sidebar.thumbnails.cache.ReaderThumbnailPreviewCacheSnapshotEventStream
import timber.log.Timber
import javax.inject.Inject

private const val SCRUB_NAVIGATION_INTERVAL_MS = 250L

@HiltViewModel
internal class ReaderScrubberViewModel @Inject constructor(
    private val thumbnailPreviewCacheUpdatedEventStream: ReaderThumbnailPreviewCacheSnapshotEventStream,
    private val thumbnailPreviewManager: ReaderThumbnailPreviewManager,
) : BaseViewModel2<ReaderScrubberViewState, ReaderScrubberViewEffect>(ReaderScrubberViewState()) {

    private var lastRequestedLandmarks: List<Int> = emptyList()
    private var pendingNavigationPage: Int? = null
    private var lastNavigationAtMs = 0L
    private var navigationJob: Job? = null

    fun initOnce() = initOnce {
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
        EventBus.getDefault().post(ReaderHistoryTrackingEvent(suspend = true))
    }

    fun onScrubTo(page: Int) {
        ignoreChangeByReaderUntil = System.currentTimeMillis() + 1000
        if (viewState.selectedPage != page) {
            updateState { copy(selectedPage = page) }
            scheduleNavigation(page)
        }
        thumbnailPreviewManager.requestThumbnail(page)
    }

    private fun scheduleNavigation(page: Int) {
        pendingNavigationPage = page
        if (navigationJob?.isActive == true) {
            return
        }
        val waitMs = (lastNavigationAtMs + SCRUB_NAVIGATION_INTERVAL_MS - SystemClock.elapsedRealtime())
            .coerceAtLeast(0L)
        navigationJob = viewModelScope.launch {
            delay(waitMs)
            postPendingNavigation()
        }
    }

    private fun postPendingNavigation() {
        val page = pendingNavigationPage ?: return
        pendingNavigationPage = null
        lastNavigationAtMs = SystemClock.elapsedRealtime()
        val location = mapOf("pageNumber" to (page + 1).toString())
        EventBus.getDefault().post(ReaderScrollReaderIfNeededEvent(location))
    }

    private fun flushNavigation() {
        navigationJob?.cancel()
        postPendingNavigation()
    }

    fun onScrubEnd() {
        flushNavigation()
        updateState { copy(isScrubbing = false) }
        EventBus.getDefault().post(ReaderHistoryTrackingEvent(suspend = false))
    }

    fun onTapAt(page: Int) {
        onScrubTo(page)
        flushNavigation()
    }

    fun onPageChangedByReader(page: Int) {
        val currentTimeMillis = System.currentTimeMillis()
        if (viewState.selectedPage == page || currentTimeMillis < ignoreChangeByReaderUntil) {
            return
        }
        updateState {
            copy(selectedPage = page)
        }
        thumbnailPreviewManager.requestThumbnail(page)
    }
}

internal data class ReaderScrubberViewState(
    val thumbnailCache: ImmutableList<Bitmap?> = persistentListOf(),
    val selectedPage: Int? = null,
    val isScrubbing: Boolean = false,
) : ViewState

internal sealed class ReaderScrubberViewEffect : ViewEffect
