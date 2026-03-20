package org.zotero.android.pdf.colorpicker

import androidx.lifecycle.viewModelScope
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.pdf.colorpicker.data.PdfReaderColor
import org.zotero.android.pdf.colorpicker.data.PdfReaderColorPickerResult
import org.zotero.android.pdf.data.PageColorLabelsMode
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import javax.inject.Inject

var queuedUpPdfReaderColorPickerResult: PdfReaderColorPickerResult? = null


@HiltViewModel
internal class PdfReaderColorPickerViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
    private val defaults: Defaults,
) : BaseViewModel2<PdfReaderColorPickerViewState, PdfReaderColorPickerViewEffect>(PdfReaderColorPickerViewState()) {

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
            val isColorLabelsEnabled= defaults.getPDFSettings().colorLabelsMode == PageColorLabelsMode.ON

            updateState {
                copy(
                    isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark,
                    isColorLabelsEnabled = isColorLabelsEnabled
                )
            }
            startObservingTheme()
            val colorPickerArgs = ScreenArguments.pdfReaderColorPickerArgs

            val colors = colors(colorPickerArgs.tool)

            val selectedColorHex = colorPickerArgs.colorHex
            val selectedColor = selectedColorHex?.let { PdfReaderColor.findByColorHex(colors = colors, hex = selectedColorHex)}
            if (selectedColorHex != null) {
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

    private fun colors(tool: AnnotationTool): List<PdfReaderColor> {
        return when (tool) {
            AnnotationTool.INK -> AnnotationsConfig.colors(AnnotationType.ink)
            AnnotationTool.NOTE -> AnnotationsConfig.colors(AnnotationType.note)
            AnnotationTool.HIGHLIGHT -> AnnotationsConfig.colors(AnnotationType.highlight)
            AnnotationTool.SQUARE -> AnnotationsConfig.colors(AnnotationType.image)
            AnnotationTool.UNDERLINE -> AnnotationsConfig.colors(AnnotationType.underline)
            AnnotationTool.FREETEXT -> AnnotationsConfig.colors(AnnotationType.text)
            else -> listOf()
        }
    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

    fun onColorSelected(color: PdfReaderColor) {
        updateState {
            copy(selectedColor = color)
        }
        updateQueueResult()
        triggerEffect(PdfReaderColorPickerViewEffect.NavigateBack)

    }

    private fun updateQueueResult() {
        queuedUpPdfReaderColorPickerResult = PdfReaderColorPickerResult(
            colorHex = viewState.selectedColor?.colorHex,
            size = viewState.size,
            annotationTool = ScreenArguments.pdfReaderColorPickerArgs.tool
        )
    }

    override fun onCleared() {
        if (queuedUpPdfReaderColorPickerResult != null) {
            EventBus.getDefault().post(queuedUpPdfReaderColorPickerResult)
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

internal data class PdfReaderColorPickerViewState(
    val selectedColor: PdfReaderColor? = null,
    val size: Float? = null,
    val colors: List<PdfReaderColor> = emptyList(),
    val isDark: Boolean = false,
    val isColorLabelsEnabled: Boolean = false,
) : ViewState

internal sealed class PdfReaderColorPickerViewEffect : ViewEffect {
    object NavigateBack : PdfReaderColorPickerViewEffect()
}
