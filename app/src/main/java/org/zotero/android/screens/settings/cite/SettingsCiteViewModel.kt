package org.zotero.android.screens.settings.cite

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapperBundle
import org.zotero.android.database.requests.ReadInstalledStylesDbRequest
import org.zotero.android.styles.data.Style
import javax.inject.Inject

@HiltViewModel
internal class SettingsCiteViewModel @Inject constructor(
    private val dispatchers: Dispatchers,
    private val dbWrapperBundle: DbWrapperBundle,
) : BaseViewModel2<SettingsCiteViewState, SettingsCiteViewEffect>(SettingsCiteViewState()) {

    fun init() = initOnce {
        viewModelScope.launch {
            val styles = loadStyles()
            updateState {
                copy(styles = styles)
            }
        }
    }

    private suspend fun loadStyles(): List<Style> = withContext(dispatchers.io) {
        val rStyles = dbWrapperBundle.realmDbStorage.perform(ReadInstalledStylesDbRequest())
        val styles = rStyles.mapNotNull { Style.fromRStyle(it) }
        styles
    }

    fun onBack() {
        triggerEffect(SettingsCiteViewEffect.OnBack)
    }

}

internal data class SettingsCiteViewState(
    val styles: List<Style> = emptyList(),
) : ViewState {
}

internal sealed class SettingsCiteViewEffect : ViewEffect {
    object OnBack : SettingsCiteViewEffect()
}