package org.zotero.android.pdf.annotation

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.pdf.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.PdfReaderThemeDecider
import javax.inject.Inject

@HiltViewModel
internal class PdfAnnotationViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
) : BaseViewModel2<PdfAnnotationViewState, PdfAnnotationViewEffect>(PdfAnnotationViewState()) {

    private var pdfReaderThemeCancellable: Job? = null

    fun init() = initOnce {
        updateState {
            copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
        }
        startObservingTheme()

        val args = ScreenArguments.pdfAnnotationArgs
        val annotation = args.selectedAnnotation!!

        updateState {
            copy(annotation = annotation)
        }
    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .onEach { data ->
                updateState {
                    copy(isDark = data!!.isDark)
                }
            }
            .launchIn(viewModelScope)
    }

}

internal data class PdfAnnotationViewState(
    val isDark: Boolean = false,
    val annotation: org.zotero.android.pdf.data.Annotation? = null

) : ViewState

internal sealed class PdfAnnotationViewEffect : ViewEffect {
}
