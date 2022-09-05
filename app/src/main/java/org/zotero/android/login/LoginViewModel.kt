package org.zotero.android.login

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.zotero.android.api.network.NetworkResultWrapper
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.framework.ZoteroApplication
import org.zotero.android.login.LoginViewEffect.NavigateToDashboard
import org.zotero.android.login.usecase.LoginUseCase
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import javax.inject.Inject

@HiltViewModel
internal class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : BaseViewModel2<LoginViewState, LoginViewEffect>(LoginViewState()) {

    fun init() = initOnce {

    }

    fun onUsernameChanged(text: String) {
        updateState {
            copy(username = text)
        }
    }

    fun onPasswordChanged(text: String) {
        updateState {
            copy(password = text)
        }
    }

    fun onSignInClicked() = viewModelScope.launch {
        if (viewState.username.isEmpty()) {
            showErrorRes(Strings.login_invalid_username)
            return@launch
        }

        if (viewState.password.isEmpty()) {
            showErrorRes(Strings.login_invalid_password)
            return@launch
        }

        updateState {
            copy(isLoading = true)
        }

        val networkResult =
            loginUseCase.execute(username = viewState.username, password = viewState.password)

        updateState {
            copy(isLoading = false)
        }

        if (networkResult !is NetworkResultWrapper.Success) {
            val error = networkResult as NetworkResultWrapper.NetworkError
            if (error.error.code == -1) {
                showErrorRes(Strings.login_invalid_credentials)
            } else {
                showError(error.error.msg)
            }
        } else {
            triggerEffect(NavigateToDashboard)
        }
    }

    private fun showError(errorText: String) {
        updateState {
            copy(
                snackbarMessage = SnackbarMessage.ErrorMessageString(
                    message = errorText,
                    onDismiss = ::dismissSnackbar
                )
            )
        }
    }

    private fun showErrorRes(errorRes: Int) {
        updateState {
            copy(
                snackbarMessage = SnackbarMessage.ErrorMessageString(
                    message = ZoteroApplication.instance.applicationContext.getString(errorRes),
                    onDismiss = ::dismissSnackbar
                )
            )
        }
    }

    private fun dismissSnackbar() {
        updateState { copy(snackbarMessage = null) }
    }

}

internal data class LoginViewState(
    val username: String = "",
    val password: String = "",
    val snackbarMessage: SnackbarMessage? = null,
    val isLoading: Boolean = false,
) : ViewState

internal sealed class LoginViewEffect : ViewEffect {
    object NavigateBack : LoginViewEffect()
    object NavigateToDashboard : LoginViewEffect()
}
