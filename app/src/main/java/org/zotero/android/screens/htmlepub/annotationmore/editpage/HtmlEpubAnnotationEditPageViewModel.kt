package org.zotero.android.screens.htmlepub.annotationmore.editpage

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
import org.zotero.android.screens.htmlepub.annotationmore.editpage.data.HtmlEpubAnnotationEditPageResult
import javax.inject.Inject

@HiltViewModel
internal class HtmlEpubAnnotationEditPageViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
) : BaseViewModel2<HtmlEpubAnnotationEditPageViewState, HtmlEpubAnnotationEditPageViewEffect>(
    HtmlEpubAnnotationEditPageViewState()
) {

    private var pdfReaderThemeCancellable: Job? = null

    fun init() = initOnce {
        updateState {
            copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
        }
        startObservingTheme()

        val args = ScreenArguments.htmlEpubAnnotationEditPageArgs

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
            HtmlEpubAnnotationEditPageResult(
                pageLabel = viewState.pageLabel
            )
        )
        triggerEffect(HtmlEpubAnnotationEditPageViewEffect.Back)
    }

    fun onValueChange(newText: String) {
        updateState {
            copy(pageLabel = newText)
        }
    }
}

internal data class HtmlEpubAnnotationEditPageViewState(
    val isDark: Boolean = false,
    val pageLabel: String = "",
) : ViewState

internal sealed class HtmlEpubAnnotationEditPageViewEffect : ViewEffect {
    object Back : HtmlEpubAnnotationEditPageViewEffect()
}
