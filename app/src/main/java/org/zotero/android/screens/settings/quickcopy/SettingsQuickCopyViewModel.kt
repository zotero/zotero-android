package org.zotero.android.screens.settings.quickcopy

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapperBundle
import org.zotero.android.database.objects.RStyle
import org.zotero.android.database.requests.ReadStyleDbRequest
import org.zotero.android.screens.settings.csllocalepicker.data.ExportLocale
import org.zotero.android.screens.settings.csllocalepicker.data.SettingsQuickCopyUpdateCslLocaleEventStream
import org.zotero.android.screens.settings.stylepicker.data.SettingsQuickCopyUpdateStyleEventStream
import org.zotero.android.styles.data.Style
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
internal class SettingsQuickCopyViewModel @Inject constructor(
    private val dispatchers: Dispatchers,
    private val dbWrapperBundle: DbWrapperBundle,
    private val defaults: Defaults,
    private val settingsQuickCopyUpdateStyleEventStream: SettingsQuickCopyUpdateStyleEventStream,
    private val settingsQuickCopyUpdateCslLocaleEventStream: SettingsQuickCopyUpdateCslLocaleEventStream,
) : BaseViewModel2<SettingsQuickCopyViewState, SettingsQuickCopyViewEffect>(
    SettingsQuickCopyViewState()
) {

    private fun setupSettingsQuickCopyReloadEventStream() {
        settingsQuickCopyUpdateStyleEventStream.flow()
            .onEach { update ->
                updateStyle(update)
            }
            .launchIn(viewModelScope)
    }

    private fun updateStyle(style: Style) {
        if (style.identifier == viewState.selectedStyle) {
            return
        }
        updateState {
            copy(selectedStyle = style.title)
        }
        defaults.setQuickCopyStyleId(style.identifier)
        val localeId = style.defaultLocale
        if (localeId != null) {
            val locToDisplay = Locale.forLanguageTag(localeId)
            val selectedLanguage = locToDisplay.getDisplayLanguage(Locale.getDefault()) ?: localeId
            updateState {
                copy(selectedLanguage = selectedLanguage)
            }
        } else {
            val locToDisplay = Locale.forLanguageTag(defaults.getQuickCopyCslLocaleId())
            val selectedLanguage = locToDisplay.getDisplayLanguage(Locale.getDefault()) ?: defaults.getQuickCopyCslLocaleId()
            updateState {
                copy(selectedLanguage = selectedLanguage)
            }
        }
    }

    fun init() = initOnce {
        setupSettingsQuickCopyReloadEventStream()
        setupSettingsQuickCopyUpdateCslLocaleEventStream()
        reload()
    }

    private fun setupSettingsQuickCopyUpdateCslLocaleEventStream() {
        settingsQuickCopyUpdateCslLocaleEventStream.flow()
            .onEach { update ->
                updateLocale(update)
            }
            .launchIn(viewModelScope)
    }

    private fun updateLocale(locale: ExportLocale) {
        defaults.setQuickCopyCslLocaleId(locale.id)
        updateState {
            copy(selectedLanguage = locale.name)
        }
    }

    private fun reload() {
        viewModelScope.launch {
            var style: RStyle? = null
            try {
                style =
                    dbWrapperBundle.realmDbStorage.perform(request = ReadStyleDbRequest(identifier = defaults.getQuickCopyStyleId()))
            } catch (e: Throwable) {
                Timber.e(e)
            }

            val language: String
            val defaultLocale = style?.defaultLocale
            if (defaultLocale != null && !defaultLocale.isEmpty()) {
                val locToDisplay = Locale.forLanguageTag(defaultLocale)
                language = locToDisplay.getDisplayLanguage(Locale.getDefault()) ?: defaultLocale
            } else {
                val localeId = defaults.getQuickCopyCslLocaleId()
                val locToDisplay = Locale.forLanguageTag(localeId)
                language = locToDisplay.getDisplayLanguage(Locale.getDefault()) ?: localeId
            }

            updateState {
                copy(
                    selectedStyle = style?.title ?: "Unknown",
                    selectedLanguage = language,
                    copyAsHtml = defaults.isQuickCopyAsHtml()
                )
            }
        }
    }

    fun onBack() {
        triggerEffect(SettingsQuickCopyViewEffect.OnBack)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    fun navigateToStylePicker() {
        triggerEffect(SettingsQuickCopyViewEffect.NavigateToStylePicker)
    }

    fun onDefaultFormatTapped() {
        navigateToStylePicker()
    }

    fun onLanguageTapped() {
        navigateToCslLocalePicker()
    }

    fun navigateToCslLocalePicker() {
        triggerEffect(SettingsQuickCopyViewEffect.NavigateToCslLocalePicker)
    }

    fun onQuickCopySwitchTapped(bool: Boolean) {
        updateState {
            copy(copyAsHtml = bool)
        }
        defaults.setQuickCopyAsHtml(bool)
    }

}

internal data class SettingsQuickCopyViewState(
    val styles: List<Style> = emptyList(),
    val selectedStyle: String = "",
    val selectedLanguage: String = "",
    val copyAsHtml: Boolean = false,
) : ViewState {
}

internal sealed class SettingsQuickCopyViewEffect : ViewEffect {
    object OnBack : SettingsQuickCopyViewEffect()
    object NavigateToStylePicker : SettingsQuickCopyViewEffect()
    object NavigateToCslLocalePicker : SettingsQuickCopyViewEffect()
}