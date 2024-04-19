package org.zotero.android.screens.root

import android.os.Bundle
import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.screens.share.ShareRawAttachmentLoader
import org.zotero.android.sync.SessionController
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val sessionController: SessionController,
    private val shareRawAttachmentLoader: ShareRawAttachmentLoader
    ) : BaseViewModel2<
        RootViewState,
        RootViewEffect>(initialState = RootViewState()) {

    fun init(extras: Bundle?) {
        if (!sessionController.isLoggedIn) {
            triggerEffect(RootViewEffect.NavigateToSignIn)
        } else if (shareRawAttachmentLoader.doesBundleContainShareData(extras)) {
            triggerEffect(RootViewEffect.NavigateToShare)
        } else {
            triggerEffect(RootViewEffect.NavigateToDashboard)
        }
    }
}

class RootViewState : ViewState

sealed class RootViewEffect : ViewEffect {
    object NavigateToSignIn : RootViewEffect()
    object NavigateToDashboard : RootViewEffect()
    object NavigateToShare : RootViewEffect()
}
