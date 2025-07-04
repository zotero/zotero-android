package org.zotero.android.screens.settings.citesearch

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapperBundle
import org.zotero.android.files.FileStore
import org.zotero.android.screens.settings.cite.SettingsCiteViewEffect
import org.zotero.android.styles.data.Style
import javax.inject.Inject

@HiltViewModel
internal class SettingsCiteSearchViewModel @Inject constructor(
    private val dispatchers: Dispatchers,
    private val dbWrapperBundle: DbWrapperBundle,
    private val defaults: Defaults,
    private  val fileStore: FileStore
) : BaseViewModel2<SettingsCiteSearchViewState, SettingsCiteViewEffect>(SettingsCiteSearchViewState()) {

    fun init() = initOnce {
        viewModelScope.launch {
            EventBus.getDefault().register(this@SettingsCiteSearchViewModel)
        }
    }


    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

}

internal data class SettingsCiteSearchViewState(
    val styles: List<Style> = emptyList(),
) : ViewState {
}

internal sealed class SettingsCiteSearchViewEffect : ViewEffect {
    object OnBack : SettingsCiteSearchViewEffect()
}