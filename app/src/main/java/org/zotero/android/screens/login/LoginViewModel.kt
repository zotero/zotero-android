package org.zotero.android.screens.login

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.zotero.android.ZoteroApplication
import org.zotero.android.api.network.CustomResult
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.screens.login.LoginViewEffect.NavigateToDashboard
import org.zotero.android.screens.login.usecase.LoginUseCase
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
            showErrorRes(Strings.errors_login_invalid_username)
            return@launch
        }

        if (viewState.password.isEmpty()) {
            showErrorRes(Strings.errors_login_invalid_password)
            return@launch
        }

        updateState {
            copy(isLoading = true)
        }

        val networkResult =
            loginUseCase.execute(username = viewState.username, password = viewState.password)

        if (networkResult !is CustomResult.GeneralSuccess) {
            updateState {
                copy(isLoading = false)
            }
            val error = networkResult as CustomResult.GeneralError.NetworkError
            if (error.isNoNetworkError()) {
                showErrorRes(Strings.errors_sync_toolbar_internet_connection)
            } else if (error.httpCode == 403) {
                showErrorRes(Strings.errors_login_invalid_credentials)
            } else {
                showError(error.stringResponse)
            }
        } else {
            triggerEffect(NavigateToDashboard)
        }
    }

    private fun showError(errorText: String?) {
        if (errorText != null) {
            updateState {
                copy(
                    snackbarMessage = SnackbarMessage.ErrorMessageString(
                        message = errorText,
                        onDismiss = ::dismissSnackbar
                    )
                )
            }
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

    fun dismissSnackbar() {
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
