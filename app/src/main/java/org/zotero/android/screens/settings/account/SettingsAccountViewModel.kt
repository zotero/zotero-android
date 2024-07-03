package org.zotero.android.screens.settings.account

import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.screens.root.RootActivity
import org.zotero.android.sync.SessionController
import org.zotero.android.webdav.WebDavSessionStorage
import org.zotero.android.webdav.data.FileSyncType
import javax.inject.Inject

@HiltViewModel
internal class SettingsAccountViewModel @Inject constructor(
    private val defaults: Defaults,
    private val sessionController: SessionController,
    private val sessionStorage: WebDavSessionStorage,
    private val context: Context
) : BaseViewModel2<SettingsAccountViewState, SettingsAccountViewEffect>(SettingsAccountViewState()) {

    fun init() = initOnce {
        updateState {
            copy(
                account = defaults.getUsername(),
                fileSyncType = if (sessionStorage.isEnabled) {
                    FileSyncType.webDav
                } else {
                    FileSyncType.zotero
                }
            )
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

    fun dismissWebDavOptionsPopup() {
        updateState {
            copy(
                showWebDavOptionsPopup = false
            )
        }
    }
    fun showWebDavOptionsPopup() {
        updateState {
            copy(
                showWebDavOptionsPopup = true
            )
        }
    }

    fun onZoteroOptionSelected() {
        dismissWebDavOptionsPopup()
    }

    fun onWebDavOptionSelected() {
        dismissWebDavOptionsPopup()
    }
}

internal data class SettingsAccountViewState(
    val account: String = "",
    val showWebDavOptionsPopup: Boolean = false,
    var fileSyncType: FileSyncType = FileSyncType.zotero,
    var markingForReupload: Boolean = false,
) : ViewState

internal sealed class SettingsAccountViewEffect : ViewEffect {
    object OnBack : SettingsAccountViewEffect()
    data class OpenWebpage(val uri: Uri) : SettingsAccountViewEffect()
}