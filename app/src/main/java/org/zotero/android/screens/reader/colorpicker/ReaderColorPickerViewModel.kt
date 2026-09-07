package org.zotero.android.screens.reader.colorpicker

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
import org.zotero.android.screens.reader.colorpicker.data.ReaderColorPickerArgs
import org.zotero.android.screens.reader.colorpicker.data.ReaderColorPickerResult
import org.zotero.android.screens.reader.data.ReaderAnnotationTool
import javax.inject.Inject

@HiltViewModel
internal class ReaderColorPickerViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
) : BaseViewModel2<ReaderColorPickerViewState, ReaderColorPickerViewEffect>(ReaderColorPickerViewState()) {

    private val screenArgs: ReaderColorPickerArgs by lazy {
        ScreenArguments.readerColorPickerArgs
    }

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

    fun init(args: ReaderColorPickerArgs?) {
        val loadedArgs = args ?: screenArgs
        initOnce {
            updateState {
                copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
            }
            startObservingTheme()
            val selectedColor = loadedArgs.colorHex
            if (selectedColor != null) {
                val colors = colors(loadedArgs.tool)
                updateState {
                    copy(
                        colors = colors,
                    )
                }
            }
            updateState {
                copy(
                    selectedColor = selectedColor,
                    size = loadedArgs.size,
                )
            }
        }
    }

    private fun colors(tool: ReaderAnnotationTool): List<String> {
        return when (tool) {
            ReaderAnnotationTool.note -> AnnotationsConfig.colors(AnnotationType.note)
            ReaderAnnotationTool.highlight -> AnnotationsConfig.colors(AnnotationType.highlight)
            ReaderAnnotationTool.underline -> AnnotationsConfig.colors(AnnotationType.underline)
            ReaderAnnotationTool.ink -> AnnotationsConfig.colors(AnnotationType.ink)
            ReaderAnnotationTool.image -> AnnotationsConfig.colors(AnnotationType.image)
            ReaderAnnotationTool.text -> AnnotationsConfig.colors(AnnotationType.text)
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
    }

      fun onSizeChanged(newSize: Float) {
        updateState {
            copy(size = newSize)
        }
    }

    fun sendColorPickerResult() {
        EventBus.getDefault().post(
            ReaderColorPickerResult(
                colorHex = viewState.selectedColor,
                size = viewState.size,
                annotationTool = ScreenArguments.readerColorPickerArgs.tool
            )
        )
    }

}

internal data class ReaderColorPickerViewState(
    val selectedColor: String? = null,
    val size: Float? = null,
    val colors: List<String> = emptyList(),
    val isDark: Boolean = false,
) : ViewState

internal sealed class ReaderColorPickerViewEffect : ViewEffect
