package org.zotero.android.pdf.annotationmore

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.pdf.annotationmore.data.PdfAnnotationMoreDeleteResult
import org.zotero.android.pdf.annotationmore.data.PdfAnnotationMoreSaveResult
import org.zotero.android.pdf.annotationmore.editpage.data.PdfAnnotationEditPageArgs
import org.zotero.android.pdf.annotationmore.editpage.data.PdfAnnotationEditPageResult
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.pdf.reader.AnnotationKey
import javax.inject.Inject

@HiltViewModel
internal class PdfAnnotationMoreViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
) : BaseViewModel2<PdfAnnotationMoreViewState, PdfAnnotationMoreViewEffect>(
    PdfAnnotationMoreViewState()
) {

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: PdfAnnotationEditPageResult) {
        updateState {
            copy(pageLabel = result.pageLabel)
        }
    }

    private var pdfReaderThemeCancellable: Job? = null

    fun init() = initOnce {
        EventBus.getDefault().register(this)
        updateState {
            copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
        }
        startObservingTheme()

        val args = ScreenArguments.pdfAnnotationMoreArgs
        val annotation = args.selectedAnnotation!!

        val colors = AnnotationsConfig.colors(annotation.type)
        updateState {
            copy(
                key = annotation.readerKey,
                type = annotation.type,
                color = annotation.color,
                colors = colors,
                lineWidth = annotation.lineWidth ?: 1.0f,
                fontSize = annotation.fontSize ?: 12f,
                highlightText = annotation.text ?: "",
                pageLabel = annotation.pageLabel,
                underlineText = annotation.text ?: "",
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

    fun onPageClicked() {
        ScreenArguments.pdfAnnotationEditPageArgs = PdfAnnotationEditPageArgs(
            pageLabel = viewState.pageLabel,
        )
        triggerEffect(PdfAnnotationMoreViewEffect.NavigateToPageEditScreen)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    fun onColorSelected(color: String) {
        updateState {
            copy(color = color)
        }
    }

    fun onSizeChanged(newSize: Float) {
        updateState {
            copy(lineWidth = newSize)
        }
    }

    fun onDeleteAnnotation() {
        EventBus.getDefault().post(
            PdfAnnotationMoreDeleteResult(
                key = viewState.key!!,
            )
        )
        triggerEffect(PdfAnnotationMoreViewEffect.Back)
    }

    fun onSave() {
        val text = when(viewState.type) {
            AnnotationType.highlight -> {
                viewState.highlightText
            }
            AnnotationType.underline -> {
                viewState.underlineText
            }
            else -> {
                ""
            }
        }
        EventBus.getDefault().post(
            PdfAnnotationMoreSaveResult(
                key = viewState.key!!,
                color = viewState.color,
                lineWidth = viewState.lineWidth,
                fontSize = viewState.fontSize,
                pageLabel = viewState.pageLabel,
                updateSubsequentLabels = viewState.updateSubsequentLabels,
                text = text
            )
        )
        triggerEffect(PdfAnnotationMoreViewEffect.Back)

    }

    fun onFontSizeDecrease() {
        updateState {
            copy(fontSize = viewState.fontSize - 0.5f)
        }

    }

    fun onFontSizeIncrease() {
        updateState {
            copy(fontSize = viewState.fontSize + 0.5f)
        }
    }

}

internal data class PdfAnnotationMoreViewState(
    val isDark: Boolean = false,
    val key: AnnotationKey? = null,
    val type: AnnotationType? = null,
    val color: String = "",
    val colors: List<String> = emptyList(),
    val lineWidth: Float = 1.0f,
    val fontSize: Float = 12f,
    val pageLabel: String = "",
    val updateSubsequentLabels: Boolean = false,
    val highlightText: String = "",
    val underlineText: String = "",
) : ViewState

internal sealed class PdfAnnotationMoreViewEffect : ViewEffect {
    object NavigateToPageEditScreen : PdfAnnotationMoreViewEffect()
    object Back : PdfAnnotationMoreViewEffect()
}
