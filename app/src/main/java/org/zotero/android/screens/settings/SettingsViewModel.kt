package org.zotero.android.screens.settings

import dagger.hilt.android.lifecycle.HiltViewModel
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val defaults: Defaults,
) : BaseViewModel2<SettingsViewState, SettingsViewEffect>(SettingsViewState()) {

    fun init() = initOnce {
        updateState {
            copy(showSubcollectionItems = defaults.showSubcollectionItems())
        }
    }

    fun setShowSubcollectionItems(showSubcollectionItems: Boolean) {
        defaults.setShowSubcollectionItems(showSubcollectionItems)
        updateState {
            copy(showSubcollectionItems = showSubcollectionItems)
        }
        EventBus.getDefault().post(EventBusConstants.ShowSubcollectionItemsChanged)
    }

    fun onDone() {
        triggerEffect(SettingsViewEffect.OnBack)
    }

    fun openPrivacyPolicy() {
        val uri = "https://www.zotero.org/support/privacy?app=1"
        triggerEffect(SettingsViewEffect.OpenWebpage(uri))
    }

    fun openSupportAndFeedback() {
        val uri = "https://forums.zotero.org/"
        triggerEffect(SettingsViewEffect.OpenWebpage(uri))
    }
}

internal data class SettingsViewState(
    val showSubcollectionItems: Boolean = false,
) : ViewState

internal sealed class SettingsViewEffect : ViewEffect {
    object OnBack : SettingsViewEffect()
    data class OpenWebpage(val url: String) : SettingsViewEffect()
}