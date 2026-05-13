package org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderWebData
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubScrollReaderIfNeededEvent
import org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails.cache.HtmlEpubThumbnailPreviewCacheSnapshotEventStream
import org.zotero.android.screens.htmlepub.reader.web.HtmlEpubReaderWebCallChainEventStream
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class HtmlEpubThumbnailsViewModel @Inject constructor(
    private val thumbnailPreviewCacheUpdatedEventStream: HtmlEpubThumbnailPreviewCacheSnapshotEventStream,
    private val thumbnailPreviewManager: HtmlEpubThumbnailPreviewManager,
    private val webCallChainEventStream: HtmlEpubReaderWebCallChainEventStream,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val dispatchers: Dispatchers,
) : BaseViewModel2<HtmlEpubThumbnailsViewState, HtmlEpubThumbnailsViewEffect>(HtmlEpubThumbnailsViewState()) {

    private var pdfReaderThemeCancellable: Job? = null

    fun initOnce(numberOfPages: Int) = initOnce {
        initNumOfPages(numberOfPages)
        startObservingTheme()
        setupWebCallChainEventStream()
        setupThumbnailCacheUpdateStream()
    }

    private fun initNumOfPages(numberOfPages: Int) {
//        if (numberOfPages == 0 || numberOfPages == viewState.numOfPages) {
//            return
//        }
        thumbnailPreviewManager.init(numOfPages = numberOfPages, viewModelScope = viewModelScope)
        val thumbnailCache = thumbnailPreviewManager.generateEmptySnapshot().toImmutableList()
        println()
        updateState {
            copy(
                numOfPages = numberOfPages,
                thumbnailCache = thumbnailCache
            )
        }

    }

    fun setupWebCallChainEventStream() {
        webCallChainEventStream.flow()
            .onEach { result ->
                process(result)
            }
            .launchIn(viewModelScope)
    }

    private suspend fun process(result: Result<HtmlEpubReaderWebData>) {
        if (result !is Result.Success) {
            return
        }

        val successValue = result.value
        when (successValue) {
            is HtmlEpubReaderWebData.onRenderThumbnail -> {
                thumbnailPreviewManager.store(
                    successValue.thumbnailJsonObject
                )
            }
            is HtmlEpubReaderWebData.onSetPageLabels -> {
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
                Timber.d("HtmlEpubThumbnailProcessing: thumbnailCache updated")
                updateState {
                    copy(thumbnailCache = cacheSnapshot)
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectThumbnail(page: Int) {
        updateState {
            copy(selectedThumbnailPage = page)
        }
        val location = mapOf("pageNumber" to page)
        EventBus.getDefault().post(HtmlEpubScrollReaderIfNeededEvent(location))
    }

    fun onPageChange(page: Int) {
        viewModelScope.launch {
            if (viewState.selectedThumbnailPage == page) {
                return@launch
            }
//            delay(100)
            updateState {
                copy(selectedThumbnailPage = page)
            }
            triggerEffect(HtmlEpubThumbnailsViewEffect.ScrollThumbnailListToIndex(page))
        }


    }

    override fun onCleared() {
//        clearThumbnailCaches()
    }

    private fun clearThumbnailCaches() {
        thumbnailPreviewManager.cancelProcessing()
        viewState.selectedThumbnailPage?.let {
            thumbnailPreviewManager.requestThumbnail(it)
        }
    }

    private fun onSetPageLabels(pageLabelsJsonArray: JsonArray) {
        updateState {
            copy(pageLabels = pageLabelsJsonArray.map { it.asString })
        }
    }

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .drop(1)
            .onEach { data ->
                clearThumbnailCaches()
//                triggerEffect(HtmlEpubReaderViewEffect.ScreenRefresh)
            }
            .launchIn(viewModelScope)
    }

    fun requestThumbnail(centerIndex: Int) {
        thumbnailPreviewManager.requestThumbnail(centerIndex)
    }
}

internal data class HtmlEpubThumbnailsViewState(
    val numOfPages: Int = 0,
    val pageLabels: List<String> = emptyList(),
    val thumbnailCache: ImmutableList<Bitmap?> = persistentListOf(),
    val selectedThumbnailPage: Int? = null,
) : ViewState {
    fun isThumbnailSelected(page: Int): Boolean {
        return this.selectedThumbnailPage == page
    }
}

internal sealed class HtmlEpubThumbnailsViewEffect : ViewEffect {
    object NavigateBack : HtmlEpubThumbnailsViewEffect()
    data class ScrollThumbnailListToIndex(val scrollToIndex: Int): HtmlEpubThumbnailsViewEffect()
}
