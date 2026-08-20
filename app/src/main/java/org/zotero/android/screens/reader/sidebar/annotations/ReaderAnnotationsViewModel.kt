package org.zotero.android.screens.reader.sidebar.annotations

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.screens.reader.sidebar.annotations.cache.ReaderAnnotationBitmapCacheSnapshotEventStream
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ReaderAnnotationsViewModel @Inject constructor(
    private val annotationBitmapCacheSnapshotEventStream: ReaderAnnotationBitmapCacheSnapshotEventStream,
    private val annotationBitmapManager: ReaderAnnotationBitmapManager,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
) : BaseViewModel2<ReaderAnnotationsViewState, ReaderAnnotationsViewEffect>(
    ReaderAnnotationsViewState()
) {

    private var pdfReaderThemeCancellable: Job? = null

    fun initOnce() = initOnce {
        startObservingTheme()
        setupAnnotationsBitmapCacheUpdateStream()
        annotationBitmapManager.init(viewModelScope)
    }

    private fun setupAnnotationsBitmapCacheUpdateStream() {
        annotationBitmapCacheSnapshotEventStream.flow()
            .onEach { cacheSnapshot ->
                Timber.d("ReaderAnnotationImageProcessing: annotationsBitmapCache updated")
                updateState {
                    copy(annotationsBitmapCache = cacheSnapshot)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun clearAnnotationsBitmapCache() {
        annotationBitmapManager.cancelProcessing()
    }

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .drop(1)
            .onEach { data ->
                clearAnnotationsBitmapCache()
            }
            .launchIn(viewModelScope)
    }

    fun requestAnnotationImage(key: String) {
        annotationBitmapManager.requestAnnotationImage(key)
    }
}

internal data class ReaderAnnotationsViewState(
    val annotationsBitmapCache: PersistentMap<String, Bitmap> = persistentMapOf(),
) : ViewState

internal sealed class ReaderAnnotationsViewEffect : ViewEffect