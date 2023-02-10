package org.zotero.android.screens.root

import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.sync.Controllers
import org.zotero.android.sync.SessionController
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val sessionController: SessionController,
    private val controller: Controllers

) : BaseViewModel2<
        RootViewState,
        RootViewEffect>(initialState = RootViewState()) {

    fun init() {
        if (sessionController.isLoggedIn) {
            triggerEffect(RootViewEffect.NavigateToDashboard)
        } else {
            triggerEffect(RootViewEffect.NavigateToSignIn)
        }
    }
}

class RootViewState : ViewState

sealed class RootViewEffect : ViewEffect {
    object NavigateToSignIn : RootViewEffect()
    object NavigateToDashboard : RootViewEffect()
}
