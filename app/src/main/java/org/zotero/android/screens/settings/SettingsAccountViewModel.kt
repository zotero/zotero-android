package org.zotero.android.screens.settings

import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.screens.root.RootActivity
import org.zotero.android.sync.SessionController
import javax.inject.Inject

@HiltViewModel
internal class SettingsAccountViewModel @Inject constructor(
    private val defaults: Defaults,
    private val sessionController: SessionController,
    private val context: Context
) : BaseViewModel2<SettingsAccountViewState, SettingsAccountViewEffect>(SettingsAccountViewState()) {

    fun init() = initOnce {
        updateState {
            copy(username = defaults.getUsername())
        }
    }

    fun onBack() {
        triggerEffect(SettingsAccountViewEffect.OnBack)
    }

    fun openDeleteAccount() {
        val uri = Uri.parse("https://www.zotero.org/settings/deleteaccount")
        triggerEffect(SettingsAccountViewEffect.OpenWebpage(uri))
    }

    fun openManageAccount() {
        val uri = Uri.parse("https://www.zotero.org/settings/account")
        triggerEffect(SettingsAccountViewEffect.OpenWebpage(uri))

    }

    fun onSignOut() {
        sessionController.reset()
        context.startActivity(RootActivity.getIntentClearTask(context))
    }
}

internal data class SettingsAccountViewState(
    val username: String = "",
) : ViewState

internal sealed class SettingsAccountViewEffect : ViewEffect {
    object OnBack : SettingsAccountViewEffect()
    data class OpenWebpage(val uri: Uri) : SettingsAccountViewEffect()
}