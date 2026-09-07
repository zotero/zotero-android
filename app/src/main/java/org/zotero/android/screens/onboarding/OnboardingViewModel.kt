package org.zotero.android.screens.onboarding

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.screens.login.data.LoginScreenArgs
import org.zotero.android.screens.login.data.RequestKind
import javax.inject.Inject

@HiltViewModel
internal class OnboardingViewModel @Inject constructor(
    private val dispatchers: Dispatchers,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
) : BaseViewModel2<OnboardingViewState, OnboardingViewEffect>(OnboardingViewState()) {

    fun navigateToSignIn() {
        navigateToLogin(RequestKind.login)
    }

    fun navigateToSignUp() {
        navigateToLogin(RequestKind.createAccount)
    }

    private fun navigateToLogin(requestKind: RequestKind) = viewModelScope.launch {
        val encodedArgs = generateArgs(requestKind)
        triggerEffect(OnboardingViewEffect.ShowLoginEffect(encodedArgs))
    }

    private suspend fun generateArgs(requestKind: RequestKind) = withContext(dispatchers.default) {
        val argsToEncode = LoginScreenArgs(requestKind)
        val encodedArgs = navigationParamsMarshaller.encodeObjectToBase64(argsToEncode)
        encodedArgs
    }

}

internal data class OnboardingViewState(
    val placeholderVar: String = "",
) : ViewState

internal sealed class OnboardingViewEffect : ViewEffect {
    data class ShowLoginEffect(val screenArgs: String) : OnboardingViewEffect()
}
