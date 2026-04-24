package org.zotero.android.screens.htmlepub.colorpicker

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
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.screens.htmlepub.colorpicker.data.HtmlEpubReaderColorPickerResult
import org.zotero.android.screens.htmlepub.reader.data.AnnotationTool
import javax.inject.Inject

var queuedUpHtmlEpubReaderColorPickerResult: HtmlEpubReaderColorPickerResult? = null

@HiltViewModel
internal class HtmlEpubReaderColorPickerViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
) : BaseViewModel2<HtmlEpubReaderColorPickerViewState, HtmlEpubReaderColorPickerViewEffect>(HtmlEpubReaderColorPickerViewState()) {

    private var pdfReaderThemeCancellable: Job? = null

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .onEach { data ->
                updateState {
                    copy(isDark = data!!.isDark)
                }
            }
            .launchIn(viewModelScope)
    }

    fun init() {
        initOnce {
            updateState {
                copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
            }
            startObservingTheme()
            val colorPickerArgs = ScreenArguments.htmlEpubReaderColorPickerArgs
            val selectedColor = colorPickerArgs.colorHex
            if (selectedColor != null) {
                val colors = colors(colorPickerArgs.tool)
                updateState {
                    copy(
                        colors = colors,
                    )
                }
            }
            updateState {
                copy(
                    selectedColor = selectedColor,
                    size = colorPickerArgs.size,
                )
            }
        }
    }

    private fun colors(tool: AnnotationTool): List<String> {
        return when (tool) {
            AnnotationTool.note -> AnnotationsConfig.colors(AnnotationType.note)
            AnnotationTool.highlight -> AnnotationsConfig.colors(AnnotationType.highlight)
            AnnotationTool.underline -> AnnotationsConfig.colors(AnnotationType.underline)
            AnnotationTool.ink -> AnnotationsConfig.colors(AnnotationType.ink)
            AnnotationTool.image -> AnnotationsConfig.colors(AnnotationType.image)
            AnnotationTool.text -> AnnotationsConfig.colors(AnnotationType.text)
            else -> listOf()
        }
    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

    fun onColorSelected(hex: String) {
        updateState {
            copy(selectedColor = hex)
        }
        updateQueueResult()
        triggerEffect(HtmlEpubReaderColorPickerViewEffect.NavigateBack)

    }

    private fun updateQueueResult() {
        queuedUpHtmlEpubReaderColorPickerResult = HtmlEpubReaderColorPickerResult(
            colorHex = viewState.selectedColor,
            size = viewState.size,
            annotationTool = ScreenArguments.htmlEpubReaderColorPickerArgs.tool
        )
    }

    override fun onCleared() {
        if (queuedUpHtmlEpubReaderColorPickerResult != null) {
            EventBus.getDefault().post(queuedUpHtmlEpubReaderColorPickerResult)
        }
        super.onCleared()
    }

      fun onSizeChanged(newSize: Float) {
        updateState {
            copy(size = newSize)
        }
        updateQueueResult()

    }

}

internal data class HtmlEpubReaderColorPickerViewState(
    val selectedColor: String? = null,
    val size: Float? = null,
    val colors: List<String> = emptyList(),
    val isDark: Boolean = false,
) : ViewState

internal sealed class HtmlEpubReaderColorPickerViewEffect : ViewEffect {
    object NavigateBack : HtmlEpubReaderColorPickerViewEffect()
}
