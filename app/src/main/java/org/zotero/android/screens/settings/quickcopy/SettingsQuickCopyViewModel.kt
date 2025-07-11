package org.zotero.android.screens.settings.quickcopy

import dagger.hilt.android.lifecycle.HiltViewModel
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.database.DbWrapperBundle
import org.zotero.android.files.FileStore
import org.zotero.android.screens.settings.citesearch.data.SettingsCiteSearchArgs
import org.zotero.android.styles.data.Style
import javax.inject.Inject

@HiltViewModel
internal class SettingsQuickCopyViewModel @Inject constructor(
    private val dispatchers: Dispatchers,
    private val dbWrapperBundle: DbWrapperBundle,
    private val defaults: Defaults,
    private val fileStore: FileStore,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
) : BaseViewModel2<SettingsQuickCopyViewState, SettingsQuickCopyViewEffect>(SettingsQuickCopyViewState()) {

    fun init() = initOnce {
    }

    fun onBack() {
        triggerEffect(SettingsQuickCopyViewEffect.OnBack)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    fun navigateToCiteSearch() {
        val installedIds = viewState.styles.map { it.identifier }.toSet()

        val args =
            SettingsCiteSearchArgs(installedIds = installedIds)
        val params = navigationParamsMarshaller.encodeObjectToBase64(args)
        triggerEffect(SettingsQuickCopyViewEffect.NavigateToCiteSearch(params))
    }

    fun onDefaultFormatTapped() {

    }

    fun onLanguageTapped() {

    }

    fun onQuickCopySwitchTapped(bool: Boolean) {

    }

}

internal data class SettingsQuickCopyViewState(
    val styles: List<Style> = emptyList(),
) : ViewState {
}

internal sealed class SettingsQuickCopyViewEffect : ViewEffect {
    object OnBack : SettingsQuickCopyViewEffect()
    data class NavigateToCiteSearch(val args: String) : SettingsQuickCopyViewEffect()
}