package org.zotero.android.screens.htmlepub.settings

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
import org.zotero.android.pdf.data.PageAppearanceMode
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.screens.htmlepub.ARG_HTML_EPUB_SETTINGS_SCREEN
import org.zotero.android.screens.htmlepub.settings.data.HtmlEpubSettings
import org.zotero.android.screens.htmlepub.settings.data.HtmlEpubSettingsArgs
import org.zotero.android.screens.htmlepub.settings.data.HtmlEpubSettingsChangeResult
import org.zotero.android.screens.htmlepub.settings.data.HtmlEpubSettingsOptions
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
internal class HtmlEpubSettingsViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    stateHandle: SavedStateHandle,
) : BaseViewModel2<HtmlEpubSettingsViewState, HtmlEpubSettingsViewEffect>(HtmlEpubSettingsViewState()) {

    private val screenArgs: HtmlEpubSettingsArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_HTML_EPUB_SETTINGS_SCREEN).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded, StandardCharsets.UTF_8)
    }

    private lateinit var htmlEpubSettings: HtmlEpubSettings
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

    fun init(args: HtmlEpubSettingsArgs?) {
        val loadedArgs = args ?: screenArgs
        initOnce {
            updateState {
                copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
            }
            startObservingTheme()
            htmlEpubSettings = loadedArgs.htmlEpubSettings
            updateState {
                copy(
                    selectedAppearanceOption = convert(htmlEpubSettings.appearanceMode),
                )
            }
        }
    }

    private fun convert(transition: PageAppearanceMode): HtmlEpubSettingsOptions {
        return when (transition) {
            PageAppearanceMode.LIGHT -> HtmlEpubSettingsOptions.AppearanceLight
            PageAppearanceMode.DARK -> HtmlEpubSettingsOptions.AppearanceDark
            PageAppearanceMode.AUTOMATIC -> HtmlEpubSettingsOptions.AppearanceAutomatic
        }
    }

    fun onOptionSelected(optionOrdinal: Int) {
        val option = HtmlEpubSettingsOptions.entries[optionOrdinal]

        when (option) {
            HtmlEpubSettingsOptions.AppearanceLight, HtmlEpubSettingsOptions.AppearanceDark, HtmlEpubSettingsOptions.AppearanceAutomatic -> {
                updateState {
                    copy(selectedAppearanceOption = option)
                }
            }
        }
        updatePdfSettings(option)

    }

    private fun updatePdfSettings(option: HtmlEpubSettingsOptions) {
        when (option) {
            HtmlEpubSettingsOptions.AppearanceLight -> {
                htmlEpubSettings.appearanceMode = PageAppearanceMode.LIGHT
            }

            HtmlEpubSettingsOptions.AppearanceDark -> {
                htmlEpubSettings.appearanceMode = PageAppearanceMode.DARK
            }

            HtmlEpubSettingsOptions.AppearanceAutomatic -> {
                htmlEpubSettings.appearanceMode = PageAppearanceMode.AUTOMATIC
            }
        }
    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

    fun sendSettingsParams() {
        EventBus.getDefault().post(HtmlEpubSettingsChangeResult(htmlEpubSettings))
    }

}

internal data class HtmlEpubSettingsViewState(
    val selectedAppearanceOption: HtmlEpubSettingsOptions = HtmlEpubSettingsOptions.AppearanceAutomatic,
    val isDark: Boolean = false,
) : ViewState {
    val appearanceOptions = listOf(
        HtmlEpubSettingsOptions.AppearanceLight,
        HtmlEpubSettingsOptions.AppearanceDark,
        HtmlEpubSettingsOptions.AppearanceAutomatic
    )

}

internal sealed class HtmlEpubSettingsViewEffect : ViewEffect
