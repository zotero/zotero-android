package org.zotero.android.screens.citbibexport

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.DbWrapperBundle
import org.zotero.android.database.requests.ReadStyleDbRequest
import org.zotero.android.screens.citbibexport.data.CitBibExportKind
import org.zotero.android.screens.citbibexport.data.CitBibExportOutputMethod
import org.zotero.android.screens.citbibexport.data.CitBibExportOutputMode
import org.zotero.android.screens.settings.csllocalepicker.data.SettingsCslLocalePickerArgs
import org.zotero.android.screens.settings.csllocalepicker.data.SettingsQuickCopyUpdateCslLocaleEventStream
import org.zotero.android.screens.settings.stylepicker.data.SettingsQuickCopyUpdateStyleEventStream
import org.zotero.android.screens.settings.stylepicker.data.SettingsStylePickerArgs
import org.zotero.android.styles.data.Style
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.uicomponents.Strings
import timber.log.Timber
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
internal class CitationBibliographyExportViewModel @Inject constructor(
    private val dbWrapperBundle: DbWrapperBundle,
    private val defaults: Defaults,
    private val context: Context,
    private val settingsQuickCopyUpdateStyleEventStream: SettingsQuickCopyUpdateStyleEventStream,
    private val settingsQuickCopyUpdateCslLocaleEventStream: SettingsQuickCopyUpdateCslLocaleEventStream,
) : BaseViewModel2<CitationBibliographyExportViewState, CitationBibliographyExportViewEffect>(
    CitationBibliographyExportViewState()
) {

    private lateinit var itemIds: Set<String>
    private lateinit var libraryId: LibraryIdentifier

    fun init(isTablet: Boolean) = initOnce {
        initViewState()
        setupSettingsQuickCopyReloadEventStream()
        setupSettingsQuickCopyUpdateCslLocaleEventStream()
        reload()
    }

    private fun initViewState() {
        val args = ScreenArguments.citationBibliographyExportArgs
        this.itemIds = args.itemIds
        this.libraryId = args.libraryId

        updateState {
            copy(
                method = defaults.getExportOutputMethod(),
                mode = defaults.getExportOutputMode()
            )
        }
    }

    fun onDone() {

    }

    fun navigateToStylePicker() {
        ScreenArguments.settingsStylePickerArgs =
            SettingsStylePickerArgs(selected = defaults.getExportStyleId())
        triggerEffect(CitationBibliographyExportViewEffect.NavigateToStylePicker)
    }

    fun onStyleTapped() {
        navigateToStylePicker()
    }

    fun onLanguageTapped() {
        navigateToCslLocalePicker()
    }

    fun navigateToCslLocalePicker() {
        ScreenArguments.settingsCslLocalePickerArgs =
            SettingsCslLocalePickerArgs(selected = defaults.getExportLocaleId())
        triggerEffect(CitationBibliographyExportViewEffect.NavigateToCslLocalePicker)
    }

    fun onOutputModeTapped() {

    }

    fun onOutputMethodTapped() {

    }

    private fun reload() {
        viewModelScope.launch {
            var style: Style? = null
            try {
                val rStyle =
                    dbWrapperBundle.realmDbStorage.perform(request = ReadStyleDbRequest(identifier = defaults.getExportStyleId()))
                style = Style.fromRStyle(rStyle = rStyle)
            } catch (e: Throwable) {
                Timber.e(e)
            }
            if (style == null) {
                style = Style(
                    identifier = "",
                    dependencyId = null,
                    title = context.getString(Strings.unknown),
                    updated = Date(),
                    href = "",
                    filename = "",
                    supportsBibliography = false,
                    isNoteStyle = false,
                    defaultLocale = null
                )
            }

            val localeId = style.defaultLocale ?: defaults.getExportLocaleId()
            val languageEnabled = style.defaultLocale == null

            var localeName: String = localeId
            if (!localeId.isEmpty()) {
                val locToDisplay = Locale.forLanguageTag(localeId)
                localeName = locToDisplay.getDisplayLanguage(Locale.getDefault()) ?: localeId
            }

            updateState {
                copy(
                    style = style,
                    localeId = localeId,
                    localeName = localeName,
                    languagePickerEnabled = languageEnabled,
                )
            }
        }
    }


    private fun setupSettingsQuickCopyReloadEventStream() {
        settingsQuickCopyUpdateStyleEventStream.flow()
            .onEach { update ->
                setStyle(update)
            }
            .launchIn(viewModelScope)
    }

    private fun setStyle(style: Style) {
        if (style.identifier == viewState.style.id) {
            return
        }
        updateState {
            copy(style = style)
        }
        defaults.setExportStyleId(style.identifier)
        val localeId = style.defaultLocale
        if (localeId != null) {
            val locToDisplay = Locale.forLanguageTag(localeId)
            val localeName = locToDisplay.getDisplayLanguage(Locale.getDefault()) ?: localeId
            updateState {
                copy(
                    localeId = localeId,
                    localeName = localeName,
                    languagePickerEnabled = false
                )
            }
        } else {
            val locToDisplay = Locale.forLanguageTag(defaults.getExportLocaleId())
            val selectedLanguage = locToDisplay.getDisplayLanguage(Locale.getDefault())
                ?: defaults.getExportLocaleId()
            updateState {
                copy(
                    localeId = defaults.getExportLocaleId(),
                    localeName = selectedLanguage,
                    languagePickerEnabled = true
                )
            }
        }
    }

    private fun setupSettingsQuickCopyUpdateCslLocaleEventStream() {
        settingsQuickCopyUpdateCslLocaleEventStream.flow()
            .onEach { update ->
                setLocale(update.id, update.name)
            }
            .launchIn(viewModelScope)
    }


    fun setMethod(method: CitBibExportOutputMethod) {
        updateState {
            copy(method = method)
        }
        defaults.setExportOutputMethod(method)
        dismissOutputMethodDialog()
    }

    fun setMode(mode: CitBibExportOutputMode) {
        updateState {
            copy(mode = mode)
        }
        defaults.setExportOutputMode(mode)
        dismissOutputModeDialog()
    }

    fun setType(type: CitBibExportKind) {
        updateState {
            copy(type = type)
        }
    }

    fun setLocale(id: String, name: String) {
        updateState {
            copy(localeId = id, localeName = name)
        }
        defaults.setExportLocaleId(id)
    }

    fun dismissOutputModeDialog() {
        updateState {
            copy(
                showOutputModeDialog = false
            )
        }
    }

    fun showOutputModeDialog() {
        updateState {
            copy(
                showOutputModeDialog = true
            )
        }
    }

    fun dismissOutputMethodDialog() {
        updateState {
            copy(
                showOutputMethodDialog = false
            )
        }
    }

    fun showOutputMethodDialog() {
        updateState {
            copy(
                showOutputMethodDialog = true
            )
        }
    }

}

internal data class CitationBibliographyExportViewState(
    val style: Style = Style(
        identifier = "",
        title = "",
        updated = Date(),
        href = "",
        filename = "",
        supportsBibliography = false,
        isNoteStyle = false,
        dependencyId = "",
        defaultLocale = ""
    ),
    val localeId: String = "",
    val localeName: String = "",
    val mode: CitBibExportOutputMode = CitBibExportOutputMode.bibliography,
    val method: CitBibExportOutputMethod = CitBibExportOutputMethod.copy,
    val type: CitBibExportKind = CitBibExportKind.cite,
    val languagePickerEnabled: Boolean = false,
    val showOutputModeDialog: Boolean = false,
    val showOutputMethodDialog: Boolean = false,
) : ViewState

internal sealed class CitationBibliographyExportViewEffect : ViewEffect {
    object OnBack : CitationBibliographyExportViewEffect()
    object NavigateToStylePicker : CitationBibliographyExportViewEffect()
    object NavigateToCslLocalePicker : CitationBibliographyExportViewEffect()
}