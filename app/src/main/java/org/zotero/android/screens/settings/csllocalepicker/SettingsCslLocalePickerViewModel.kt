package org.zotero.android.screens.settings.csllocalepicker

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.locales.ExportCslLocaleReader
import org.zotero.android.screens.settings.csllocalepicker.data.ExportLocale
import org.zotero.android.screens.settings.csllocalepicker.data.SettingsQuickCopyUpdateCslLocaleEventStream
import javax.inject.Inject

@HiltViewModel
internal class SettingsCslLocalePickerViewModel @Inject constructor(
    private val dispatchers: Dispatchers,
    private val exportCslLocaleReader: ExportCslLocaleReader,
    private val settingsQuickCopyUpdateCslLocaleEventStream: SettingsQuickCopyUpdateCslLocaleEventStream,
) : BaseViewModel2<SettingsCslLocalePickerViewState, SettingsCslLocalePickerViewEffect>(
    SettingsCslLocalePickerViewState()
) {

    fun init() = initOnce {
        val selected = ScreenArguments.settingsCslLocalePickerArgs.selected
        updateState {
            copy(selected = selected)
        }

        viewModelScope.launch {
            reloadCslLocales()
        }
    }

    private suspend fun reloadCslLocales() {
        val locales = loadCslLocales()
        updateState {
            copy(locales = locales.toPersistentList())
        }
    }

    private suspend fun loadCslLocales(): List<ExportLocale> = withContext(dispatchers.io) {
        val locales = exportCslLocaleReader.load()
        locales
    }

    fun onBack() {
        triggerEffect(SettingsCslLocalePickerViewEffect.OnBack)
    }

    fun onItemTapped(locale: ExportLocale) {
        settingsQuickCopyUpdateCslLocaleEventStream.emitAsync(locale)
        triggerEffect(SettingsCslLocalePickerViewEffect.OnBack)
    }

}

internal data class SettingsCslLocalePickerViewState(
    val locales: PersistentList<ExportLocale> = persistentListOf(),
    val selected: String = ""
) : ViewState

internal sealed class SettingsCslLocalePickerViewEffect : ViewEffect {
    object OnBack : SettingsCslLocalePickerViewEffect()
}