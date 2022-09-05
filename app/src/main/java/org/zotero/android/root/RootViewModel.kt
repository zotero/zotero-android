package org.zotero.android.root

import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.root.RootViewEffect.NavigateToRoom
import org.zotero.android.root.RootViewEffect.NavigateToSignIn
import org.zotero.android.root.usecase.UserIsLoggedInUseCase
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val userIsLoggedIn: UserIsLoggedInUseCase,
) : BaseViewModel2<
        RootViewState,
        RootViewEffect>(initialState = RootViewState()) {

    fun init() {
        if (!userIsLoggedIn.execute()) {
            triggerEffect(NavigateToSignIn)
        } else {
            triggerEffect(NavigateToRoom)
        }
    }
}

class RootViewState : ViewState

sealed class RootViewEffect : ViewEffect {
    object NavigateToSignIn : RootViewEffect()
    object NavigateToRoom : RootViewEffect()
}
