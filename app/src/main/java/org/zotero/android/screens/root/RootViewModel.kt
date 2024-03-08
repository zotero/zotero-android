package org.zotero.android.screens.root

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.sync.SessionController
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val sessionController: SessionController,
    private val stateHandle: SavedStateHandle,

    ) : BaseViewModel2<
        RootViewState,
        RootViewEffect>(initialState = RootViewState()) {

    fun init() {
        if (!sessionController.isLoggedIn) {
            triggerEffect(RootViewEffect.NavigateToSignIn)
        } else if (stateHandle.keys().isNotEmpty()) {
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
