package org.zotero.android.screens.login

import android.content.Context
import android.webkit.WebView
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.zotero.android.ZoteroApplication
import org.zotero.android.api.AuthApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.screens.login.data.LoginError
import org.zotero.android.screens.login.data.SessionStatus
import org.zotero.android.sync.SessionController
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import org.zotero.android.websocket.LoginSessionWebSocketController
import org.zotero.android.websocket.mappers.CheckLoginSessionResponseMapper
import org.zotero.android.websocket.mappers.CreateLoginSessionResponseMapper
import org.zotero.android.websocket.responses.CheckLoginSessionResponse
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class LoginViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val context: Context,
    private val createLoginSessionResponseMapper: CreateLoginSessionResponseMapper,
    private val checkLoginSessionResponseMapper: CheckLoginSessionResponseMapper,
    private val webSocketController: LoginSessionWebSocketController,
    private val sessionController: SessionController,
    private val dispatchers: Dispatchers,
) : BaseViewModel2<LoginViewState, LoginViewEffect>(LoginViewState()) {

    var sessionStatus: SessionStatus? = null
    var sessionToken: String? = null
    var loginUrl: String? = null

    private var loginSocketMessageDisposable: Job? = null
    private var pollingDisposable: Job? = null

    var webView: WebView? = null

    fun init(webView: WebView) {
        this.webView = webView
        viewModelScope.launch {
            login()
        }
    }

    private suspend fun login() {
        if (this.sessionStatus != null) {
            return
        }
        this.sessionStatus = SessionStatus.creating
        this.sessionToken = null
        this.loginUrl = null

        val result = safeApiCall {
            authApi.createLoginSessionRequest()
        }

        if (result is CustomResult.GeneralSuccess) {
            val createLoginSessionResponse = createLoginSessionResponseMapper.fromJson(result.value!!)
            this.sessionStatus = SessionStatus.checking
            this.sessionToken = createLoginSessionResponse.sessionToken
            this.loginUrl = createLoginSessionResponse.loginUrl + "&app=1"
            this.webView?.loadUrl(this.loginUrl!!)
            startStreaming(sessionToken!!)
            startSessionPolling(sessionToken!!)
        } else {
            this.sessionStatus = null
            result as CustomResult.GeneralError
            val errorText = loginError(result).localizedDescription(context)
            Timber.e("LoginViewModel: could not create login session: $errorText")
            showError(errorText)
        }
    }

    private fun handleStartSessionPollingError(error: CustomResult.GeneralError, token: String) {
        when(error) {
            is CustomResult.GeneralError.CodeError -> {
                if(error.throwable is CancellationException) {
                    return
                }
                Timber.e(error.throwable,"LoginViewModel: could not poll login session")
            }
            is CustomResult.GeneralError.NetworkError -> {
                Timber.e("LoginViewModel: could not poll login session: ${error.stringResponse}")
            }
        }
        this.sessionStatus = null
        showError(loginError(error).localizedDescription(context))
        stopSessionMonitoring(token)
    }


    private fun startSessionPolling(
        token: String,
    ) {
        val timeOutTargetMs: Long = System.currentTimeMillis() + (10 * 60 * 1000L)
        pollingDisposable = CoroutineScope(dispatchers.io).launch {
            try {
                while (isActive) {
                    val jsonResponse =
                        safeApiCall {
                            authApi.checkLoginSessionRequest(token)
                        }
                    if (jsonResponse is CustomResult.GeneralSuccess) {
                        val response =
                            checkLoginSessionResponseMapper.fromJson(jsonResponse.value!!)
                        when (response.status) {
                            CheckLoginSessionResponse.Status.pending -> {
                                //no-op
                            }

                            is CheckLoginSessionResponse.Status.completed -> {
                                if (this@LoginViewModel.sessionStatus == SessionStatus.checking) {
                                    this@LoginViewModel.sessionStatus = SessionStatus.completed
                                    stopSessionMonitoring(token)
                                    sessionController.register(
                                        userId = response.status.userId,
                                        username = response.status.username,
                                        displayName = "",
                                        apiToken = response.status.apiKey
                                    )
                                }
                                viewModelScope.launch {
                                    triggerEffect(LoginViewEffect.NavigateToDashboard)
                                }
                            }

                            CheckLoginSessionResponse.Status.cancelled -> {
                                stopSessionMonitoring(token)
                                reset()
                                viewModelScope.launch {
                                    triggerEffect(LoginViewEffect.NavigateBack)
                                }
                            }
                        }
                    }
                    if (System.currentTimeMillis() > timeOutTargetMs) {
                        throw LoginError.sessionTimedOut
                    }
                    delay(3_000)
                }
            } catch (e: Exception) {
                handleStartSessionPollingError(
                    error = CustomResult.GeneralError.CodeError(e),
                    token = token
                )
            }
        }
    }

    private fun startStreaming(token: String) {
        val topic = LoginSessionWebSocketController.topic( token)
        this.loginSocketMessageDisposable = webSocketController.loginObservable
            .flow()
            .filter({ it.topic == topic })
            .take(1)
            .onEach { response ->
                if (this.sessionStatus != SessionStatus.checking) {
                    return@onEach
                }
                this.sessionStatus = SessionStatus.completed
                stopSessionMonitoring(token)
                sessionController.register(userId = response.userId, username = response.username, displayName = "", apiToken = response.apiKey)
                triggerEffect(LoginViewEffect.NavigateToDashboard)
            }.launchIn(viewModelScope)

        webSocketController.init()
        webSocketController.connect(token)

    }

    private fun stopSessionMonitoring(token: String?) {
        pollingDisposable?.cancel()
        pollingDisposable = null

        loginSocketMessageDisposable?.cancel()
        loginSocketMessageDisposable = null

        webSocketController.disconnect(token)
    }

    private fun reset() {
        this.sessionStatus = null
        this.sessionToken = null
        this.loginUrl = null
    }

    private fun showError(errorText: String?) = viewModelScope.launch {
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

    private fun loginError(error: CustomResult.GeneralError): LoginError {
        when (error) {
            is CustomResult.GeneralError.NetworkError -> {
                if (error.isNoNetworkError()) {
                    return LoginError.serverError(context.getString(Strings.errors_sync_toolbar_internet_connection))
                } else {
                    val stringResponse = error.stringResponse
                    return if (!stringResponse.isNullOrEmpty()) {
                        LoginError.serverError(stringResponse)
                    } else {
                        LoginError.serverError("Unknown network error")
                    }
                }
            }

            is CustomResult.GeneralError.CodeError -> {
                if (error.throwable is LoginError.sessionTimedOut) {
                    return error.throwable
                }
                return LoginError.unknown(error.throwable)
            }
        }
    }

    override fun onCleared() {
        stopSessionMonitoring(this.sessionToken)
        cancelLoginSessionIfNeeded()
    }

    private fun cancelLoginSessionIfNeeded() = CoroutineScope(dispatchers.main).launch {
        if (this@LoginViewModel.sessionStatus != SessionStatus.checking) {
            return@launch
        }
        val token = this@LoginViewModel.sessionToken
        if (token == null) {
            reset()
            return@launch
        }
        this@LoginViewModel.sessionStatus = SessionStatus.cancelling

        val response = safeApiCall { authApi.submitCancelLoginSessionRequest(token)}
        if (response is CustomResult.GeneralSuccess) {
            Timber.i("LoginViewModel: cancelled session")
            reset()
        } else {
            response as CustomResult.GeneralError
            Timber.w("LoginViewModel: could not cancel session - ${loginError(response)}")
            reset()
        }
    }
}

internal data class LoginViewState(
    val snackbarMessage: SnackbarMessage? = null,
) : ViewState

internal sealed class LoginViewEffect : ViewEffect {
    object NavigateBack : LoginViewEffect()
    object NavigateToDashboard : LoginViewEffect()
}
