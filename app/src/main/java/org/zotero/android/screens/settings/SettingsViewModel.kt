package org.zotero.android.screens.settings

import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val defaults: Defaults
) : BaseViewModel2<SettingsViewState, SettingsViewEffect>(SettingsViewState()) {

    fun init(){
        updateState {
            copy(showSubcollectionItems = defaults.showSubcollectionItems())
        }
    }

    fun onDone() {
        triggerEffect(SettingsViewEffect.OnBack)
    }

    fun openPrivacyPolicy() {
        val uri = "https://www.zotero.org/support/privacy?app=1".toUri()
        triggerEffect(SettingsViewEffect.OpenWebpage(uri))
    }

    fun openSupportAndFeedback() {
        val uri = "https://forums.zotero.org/".toUri()
        triggerEffect(SettingsViewEffect.OpenWebpage(uri))
    }

    fun onShowSubcollectionItemsChanged(newValue: Boolean) {
        defaults.setShowSubcollectionItems(newValue)
        updateState {
            copy(showSubcollectionItems = newValue)
        }
    }
}

internal data class SettingsViewState(
    val placeholder: String = "",
    val showSubcollectionItems: Boolean = true
) : ViewState

internal sealed class SettingsViewEffect : ViewEffect {
    object OnBack : SettingsViewEffect()
    data class OpenWebpage(val uri: Uri) : SettingsViewEffect()
}
