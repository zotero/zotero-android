package org.zotero.android.screens.reader.annotationmore.editpage

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.screens.reader.annotationmore.editpage.data.ReaderAnnotationEditPageResult
import javax.inject.Inject

@HiltViewModel
internal class ReaderAnnotationEditPageViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
) : BaseViewModel2<ReaderAnnotationEditPageViewState, ReaderAnnotationEditPageViewEffect>(
    ReaderAnnotationEditPageViewState()
) {

    private var pdfReaderThemeCancellable: Job? = null

    fun init() = initOnce {
        updateState {
            copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
        }
        startObservingTheme()

        val args = ScreenArguments.readerAnnotationEditPageArgs

        updateState {
            copy(
                pageLabel = args.pageLabel,
            )
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

    fun onBack() {
        EventBus.getDefault().post(
            ReaderAnnotationEditPageResult(
                pageLabel = viewState.pageLabel
            )
        )
        triggerEffect(ReaderAnnotationEditPageViewEffect.Back)
    }

    fun onValueChange(newText: String) {
        updateState {
            copy(pageLabel = newText)
        }
    }
}

internal data class ReaderAnnotationEditPageViewState(
    val isDark: Boolean = false,
    val pageLabel: String = "",
) : ViewState

internal sealed class ReaderAnnotationEditPageViewEffect : ViewEffect {
    object Back : ReaderAnnotationEditPageViewEffect()
}
