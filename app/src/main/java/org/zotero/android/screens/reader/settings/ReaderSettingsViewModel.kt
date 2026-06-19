package org.zotero.android.screens.reader.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.screens.reader.ARG_READER_SETTINGS_SCREEN
import org.zotero.android.screens.reader.data.ReaderFileType
import org.zotero.android.screens.reader.settings.data.PageAppearanceMode
import org.zotero.android.screens.reader.settings.data.PageLayoutFlowMode
import org.zotero.android.screens.reader.settings.data.PageScrollDirection
import org.zotero.android.screens.reader.settings.data.PageSpreadsMode
import org.zotero.android.screens.reader.settings.data.ReaderSettings
import org.zotero.android.screens.reader.settings.data.ReaderSettingsArgs
import org.zotero.android.screens.reader.settings.data.ReaderSettingsChangeResult
import org.zotero.android.screens.reader.settings.data.ReaderSettingsOptions
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
internal class ReaderSettingsViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    stateHandle: SavedStateHandle,
) : BaseViewModel2<ReaderSettingsViewState, ReaderSettingsViewEffect>(ReaderSettingsViewState()) {

    private val screenArgs: ReaderSettingsArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_READER_SETTINGS_SCREEN).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded, StandardCharsets.UTF_8)
    }

    private lateinit var readerSettings: ReaderSettings
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

    fun init(args: ReaderSettingsArgs?) {
        val loadedArgs = args ?: screenArgs
        initOnce {
            updateState {
                copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
            }
            startObservingTheme()
            readerSettings = loadedArgs.readerSettings
            updateState {
                copy(
                    selectedAppearanceOption = convert(readerSettings.appearanceMode),
                    selectedScrollDirectionOption = convert(readerSettings.direction),
                    selectedSpreadsOption = convert(readerSettings.spreadsMode),
                    selectedPageLayoutFlowMode = convert(readerSettings.pageLayoutFlowMode),
                    fileType = loadedArgs.fileType,
                )
            }
        }
    }

    private fun convert(transition: PageAppearanceMode): ReaderSettingsOptions {
        return when (transition) {
            PageAppearanceMode.LIGHT -> {
                ReaderSettingsOptions.AppearanceLight
            }
            PageAppearanceMode.DARK -> {
                ReaderSettingsOptions.AppearanceDark
            }
            PageAppearanceMode.AUTOMATIC -> {
                ReaderSettingsOptions.AppearanceAutomatic
            }
        }
    }

    private fun convert(direction: PageScrollDirection): ReaderSettingsOptions {
        return when (direction) {
            PageScrollDirection.HORIZONTAL -> {
                ReaderSettingsOptions.ScrollDirectionHorizontal
            }
            PageScrollDirection.VERTICAL -> {
                ReaderSettingsOptions.ScrollDirectionVertical
            }
        }
    }

    private fun convert(spreadsMode: PageSpreadsMode): ReaderSettingsOptions {
        return when (spreadsMode) {
            PageSpreadsMode.NONE -> {
                ReaderSettingsOptions.PageSpreadsNone
            }
            PageSpreadsMode.ODD -> {
                ReaderSettingsOptions.PageSpreadsDouble
            }
            PageSpreadsMode.EVEN -> {
                ReaderSettingsOptions.PageSpreadsEven
            }
        }
    }

    private fun convert(flowMode: PageLayoutFlowMode): ReaderSettingsOptions {
        return when (flowMode) {
            PageLayoutFlowMode.PAGINATED -> {
                ReaderSettingsOptions.PageLayoutFlowModePaginated
            }
            PageLayoutFlowMode.SCROLLED -> {
                ReaderSettingsOptions.PageLayoutFlowModeScrolled
            }
        }
    }

    fun onOptionSelected(optionOrdinal: Int) {
        val option = ReaderSettingsOptions.entries[optionOrdinal]

        when (option) {
            ReaderSettingsOptions.AppearanceLight, ReaderSettingsOptions.AppearanceDark, ReaderSettingsOptions.AppearanceAutomatic -> {
                updateState {
                    copy(selectedAppearanceOption = option)
                }
            }
            ReaderSettingsOptions.ScrollDirectionHorizontal, ReaderSettingsOptions.ScrollDirectionVertical -> {
                updateState {
                    copy(selectedScrollDirectionOption = option)
                }
            }
            ReaderSettingsOptions.PageSpreadsNone, ReaderSettingsOptions.PageSpreadsDouble, ReaderSettingsOptions.PageSpreadsEven -> {
                updateState {
                    copy(selectedSpreadsOption = option)
                }
            }

            ReaderSettingsOptions.PageLayoutFlowModePaginated, ReaderSettingsOptions.PageLayoutFlowModeScrolled -> {
                updateState {
                    copy(selectedPageLayoutFlowMode = option)
                }
            }
        }
        updatePdfSettings(option)

    }

    private fun updatePdfSettings(option: ReaderSettingsOptions) {
        when (option) {
            ReaderSettingsOptions.AppearanceLight -> {
                readerSettings.appearanceMode = PageAppearanceMode.LIGHT
            }

            ReaderSettingsOptions.AppearanceDark -> {
                readerSettings.appearanceMode = PageAppearanceMode.DARK
            }

            ReaderSettingsOptions.AppearanceAutomatic -> {
                readerSettings.appearanceMode = PageAppearanceMode.AUTOMATIC
            }

            ReaderSettingsOptions.ScrollDirectionHorizontal -> {
                readerSettings.direction = PageScrollDirection.HORIZONTAL
            }
            ReaderSettingsOptions.ScrollDirectionVertical -> {
                readerSettings.direction = PageScrollDirection.VERTICAL
            }

            ReaderSettingsOptions.PageSpreadsNone -> {
                readerSettings.spreadsMode = PageSpreadsMode.NONE
            }
            ReaderSettingsOptions.PageSpreadsDouble -> {
                readerSettings.spreadsMode = PageSpreadsMode.ODD
            }
            ReaderSettingsOptions.PageSpreadsEven -> {
                readerSettings.spreadsMode = PageSpreadsMode.EVEN
            }

            ReaderSettingsOptions.PageLayoutFlowModePaginated -> {
                readerSettings.pageLayoutFlowMode = PageLayoutFlowMode.PAGINATED
            }
            ReaderSettingsOptions.PageLayoutFlowModeScrolled -> {
                readerSettings.pageLayoutFlowMode = PageLayoutFlowMode.SCROLLED
            }
        }
    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

    fun sendSettingsParams() {
        EventBus.getDefault().post(ReaderSettingsChangeResult(readerSettings))
    }

}

internal data class ReaderSettingsViewState(
    val selectedAppearanceOption: ReaderSettingsOptions = ReaderSettingsOptions.AppearanceAutomatic,
    val selectedScrollDirectionOption: ReaderSettingsOptions = ReaderSettingsOptions.ScrollDirectionHorizontal,
    val selectedSpreadsOption: ReaderSettingsOptions = ReaderSettingsOptions.PageSpreadsNone,
    val selectedPageLayoutFlowMode: ReaderSettingsOptions = ReaderSettingsOptions.PageLayoutFlowModePaginated,
    val isDark: Boolean = false,
    val fileType: ReaderFileType = ReaderFileType.EPUB
) : ViewState {
    val scrollDirectionOptions = listOf(
        ReaderSettingsOptions.ScrollDirectionHorizontal, ReaderSettingsOptions.ScrollDirectionVertical
    )
    val appearanceOptions = listOf(
        ReaderSettingsOptions.AppearanceLight,
        ReaderSettingsOptions.AppearanceDark,
        ReaderSettingsOptions.AppearanceAutomatic
    )

    val spreadsOptions = listOf(
        ReaderSettingsOptions.PageSpreadsNone,
        ReaderSettingsOptions.PageSpreadsDouble,
        ReaderSettingsOptions.PageSpreadsEven
    )
    val pageLayoutFlowOptions = listOf(
        ReaderSettingsOptions.PageLayoutFlowModePaginated,
        ReaderSettingsOptions.PageLayoutFlowModeScrolled,
    )

}

internal sealed class ReaderSettingsViewEffect : ViewEffect
