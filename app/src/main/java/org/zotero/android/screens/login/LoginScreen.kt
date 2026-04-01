package org.zotero.android.screens.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.login.LoginViewEffect.NavigateBack
import org.zotero.android.screens.login.LoginViewEffect.NavigateToDashboard
import org.zotero.android.uicomponents.CustomScaffoldM3

@Composable
internal fun LoginScreen(
    onBack: () -> Unit,
    navigateToDashboard: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewStates.observeAsState(LoginViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()

    LaunchedEffect(key1 = viewEffect) {
        when (viewEffect?.consume()) {
            NavigateBack -> onBack()
            NavigateToDashboard -> navigateToDashboard()
            null -> Unit
        }
    }
    CustomScaffoldM3(
        topBar = {
            LoginTopBar(
                onCancelClicked = onBack,
            )
        },
        snackbarMessage = viewState.snackbarMessage,
    ) {
        LoginWebView(viewModel)
    }
}