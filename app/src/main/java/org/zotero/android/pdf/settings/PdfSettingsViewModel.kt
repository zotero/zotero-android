package org.zotero.android.pdf.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.pdf.data.PDFSettings
import org.zotero.android.pdf.data.PageAppearanceMode
import org.zotero.android.pdf.data.PageFitting
import org.zotero.android.pdf.data.PageLayoutMode
import org.zotero.android.pdf.data.PageScrollDirection
import org.zotero.android.pdf.data.PageScrollMode
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.pdf.settings.data.PdfSettingsArgs
import org.zotero.android.pdf.settings.data.PdfSettingsChangeResult
import org.zotero.android.pdf.settings.data.PdfSettingsOptions
import javax.inject.Inject

@HiltViewModel
internal class PdfSettingsViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
) : BaseViewModel2<PdfSettingsViewState, PdfSettingsViewEffect>(PdfSettingsViewState()) {

    private lateinit var pdfSettings: PDFSettings
    private var pdfReaderThemeCancellable: Job? = null
    lateinit var args: PdfSettingsArgs

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .onEach { data ->
                updateState {
                    copy(isDark = data!!.isDark)
                }
            }
            .launchIn(viewModelScope)
    }

    fun init(args: PdfSettingsArgs) {
        initOnce {
            updateState {
                copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
            }
            startObservingTheme()
            pdfSettings = args.pdfSettings
            updateState {
                copy(
                    selectedPageTransitionOption = convert(pdfSettings.transition),
                    selectedAppearanceOption = convert(pdfSettings.appearanceMode),
                    selectedPageFittingOption = convert(pdfSettings.pageFitting),
                    selectedPageModeOption = convert(pdfSettings.pageMode),
                    selectedScrollDirectionOption = convert(pdfSettings.direction)
                )
            }
        }
    }

    private fun convert(direction: PageScrollDirection): PdfSettingsOptions {
        return when (direction) {
            PageScrollDirection.HORIZONTAL -> PdfSettingsOptions.ScrollDirectionHorizontal
            PageScrollDirection.VERTICAL -> PdfSettingsOptions.ScrollDirectionVertical
        }

    }

    private fun convert(pageMode: PageLayoutMode): PdfSettingsOptions {
        return when(pageMode) {
            PageLayoutMode.SINGLE -> PdfSettingsOptions.PageModeSingle
            PageLayoutMode.DOUBLE -> PdfSettingsOptions.PageModeDouble
            PageLayoutMode.AUTOMATIC -> PdfSettingsOptions.PageModeAutomatic
        }
    }

    private fun convert(pageFitting: PageFitting): PdfSettingsOptions {
        return when(pageFitting) {
            PageFitting.FIT -> PdfSettingsOptions.PageFittingFit
            PageFitting.FILL -> PdfSettingsOptions.PageFittingFill
        }
    }

    private fun convert(transition: PageAppearanceMode): PdfSettingsOptions {
        return when (transition) {
            PageAppearanceMode.LIGHT -> PdfSettingsOptions.AppearanceLight
            PageAppearanceMode.DARK -> PdfSettingsOptions.AppearanceDark
            PageAppearanceMode.AUTOMATIC -> PdfSettingsOptions.AppearanceAutomatic
        }
    }

    private fun convert(transition: PageScrollMode): PdfSettingsOptions {
        return when (transition) {
            PageScrollMode.JUMP -> PdfSettingsOptions.PageTransitionJump
            PageScrollMode.CONTINUOUS -> PdfSettingsOptions.PageTransitionContinuous
        }
    }

    fun onOptionSelected(optionOrdinal: Int) {
        val option = PdfSettingsOptions.values()[optionOrdinal]

        when (option) {
            PdfSettingsOptions.PageTransitionJump, PdfSettingsOptions.PageTransitionContinuous -> {
                updateState {
                    copy(selectedPageTransitionOption = option)
                }
            }
            PdfSettingsOptions.PageModeSingle, PdfSettingsOptions.PageModeDouble, PdfSettingsOptions.PageModeAutomatic -> {
                updateState {
                    copy(selectedPageModeOption = option)
                }
            }
            PdfSettingsOptions.ScrollDirectionHorizontal, PdfSettingsOptions.ScrollDirectionVertical -> {
                updateState {
                    copy(selectedScrollDirectionOption = option)
                }
            }
            PdfSettingsOptions.PageFittingFit, PdfSettingsOptions.PageFittingFill -> {
                updateState {
                    copy(selectedPageFittingOption = option)
                }
            }
            PdfSettingsOptions.AppearanceLight, PdfSettingsOptions.AppearanceDark, PdfSettingsOptions.AppearanceAutomatic -> {
                updateState {
                    copy(selectedAppearanceOption = option)
                }
            }
        }
        sendChangedSettings(option)

    }

    private fun sendChangedSettings(option: PdfSettingsOptions) {
        when (option) {
            PdfSettingsOptions.PageTransitionJump -> {
                pdfSettings.transition = PageScrollMode.JUMP
            }

            PdfSettingsOptions.PageTransitionContinuous -> {
                pdfSettings.transition = PageScrollMode.CONTINUOUS
            }

            PdfSettingsOptions.PageModeSingle -> {
                pdfSettings.pageMode = PageLayoutMode.SINGLE
            }

            PdfSettingsOptions.PageModeDouble -> {
                pdfSettings.pageMode = PageLayoutMode.DOUBLE
            }

            PdfSettingsOptions.PageModeAutomatic -> {
                pdfSettings.pageMode = PageLayoutMode.AUTOMATIC
            }

            PdfSettingsOptions.ScrollDirectionHorizontal -> {
                pdfSettings.direction = PageScrollDirection.HORIZONTAL
            }

            PdfSettingsOptions.ScrollDirectionVertical -> {
                pdfSettings.direction = PageScrollDirection.VERTICAL
            }

            PdfSettingsOptions.PageFittingFit -> {
                pdfSettings.pageFitting = PageFitting.FIT
            }

            PdfSettingsOptions.PageFittingFill -> {
                pdfSettings.pageFitting = PageFitting.FILL
            }

            PdfSettingsOptions.AppearanceLight -> {
                pdfSettings.appearanceMode = PageAppearanceMode.LIGHT
            }

            PdfSettingsOptions.AppearanceDark -> {
                pdfSettings.appearanceMode = PageAppearanceMode.DARK
            }

            PdfSettingsOptions.AppearanceAutomatic -> {
                pdfSettings.appearanceMode = PageAppearanceMode.AUTOMATIC
            }
        }

        EventBus.getDefault().post(PdfSettingsChangeResult(pdfSettings))
    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

}

internal data class PdfSettingsViewState(
    val selectedPageTransitionOption: PdfSettingsOptions = PdfSettingsOptions.PageTransitionJump,
    val selectedPageModeOption: PdfSettingsOptions = PdfSettingsOptions.PageModeSingle,
    val selectedScrollDirectionOption: PdfSettingsOptions = PdfSettingsOptions.ScrollDirectionHorizontal,
    val selectedPageFittingOption: PdfSettingsOptions = PdfSettingsOptions.PageFittingFit,
    val selectedAppearanceOption: PdfSettingsOptions = PdfSettingsOptions.AppearanceAutomatic,
    val isDark: Boolean = false,
) : ViewState {

    val pageTransitionOptions = listOf(
        PdfSettingsOptions.PageTransitionJump,
        PdfSettingsOptions.PageTransitionContinuous
    )
    val pageModeOptions = listOf(
        PdfSettingsOptions.PageModeSingle,
        PdfSettingsOptions.PageModeDouble,
        PdfSettingsOptions.PageModeAutomatic
    )

    val scrollDirectionOptions = listOf(
        PdfSettingsOptions.ScrollDirectionHorizontal, PdfSettingsOptions.ScrollDirectionVertical
    )
    val pageFittingsOptions = listOf(
        PdfSettingsOptions.PageFittingFit,
        PdfSettingsOptions.PageFittingFill,
    )
    val appearanceOptions = listOf(
        PdfSettingsOptions.AppearanceLight,
        PdfSettingsOptions.AppearanceDark,
        PdfSettingsOptions.AppearanceAutomatic
    )

}

internal sealed class PdfSettingsViewEffect : ViewEffect {
    object NavigateBack : PdfSettingsViewEffect()
}
