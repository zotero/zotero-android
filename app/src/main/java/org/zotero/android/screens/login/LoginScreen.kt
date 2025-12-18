package org.zotero.android.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.login.LoginViewEffect.NavigateBack
import org.zotero.android.screens.login.LoginViewEffect.NavigateToDashboard
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.button.CustomFilledButton
import org.zotero.android.uicomponents.button.CustomTextButton
import org.zotero.android.uicomponents.textinput.CustomOutlineTextField

@Composable
internal fun LoginScreen(
    onBack: () -> Unit,
    navigateToDashboard: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewStates.observeAsState(LoginViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top,
            ) {
                val focusManager = LocalFocusManager.current
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                val moveFocusDownAction = {
                    focusManager.moveFocus(FocusDirection.Down)
                }
                CustomOutlineTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = viewState.username,
                    placeholderText = stringResource(id = Strings.login_username),
                    labelText = stringResource(id = Strings.login_username),
                    onValueChange = viewModel::onUsernameChanged,
                    maxLines = 1,
                    singleLine = true,
                    focusRequester = focusRequester,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { moveFocusDownAction() }
                    ),
                    onEnterOrTab = { moveFocusDownAction() },
                    semanticsModifier = Modifier.semantics { contentType = ContentType.Username }
                )
                Spacer(modifier = Modifier.height(12.dp))
                CustomOutlineTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = viewState.password,
                    placeholderText = stringResource(id = Strings.login_password),
                    labelText = stringResource(id = Strings.login_password),
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = viewModel::onPasswordChanged,
                    maxLines = 1,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { viewModel.onSignInClicked() }
                    ),
                    onEnterOrTab = {
                        viewModel.onSignInClicked()
                    },
                    semanticsModifier = Modifier.semantics { contentType = ContentType.Password }
                )
                Spacer(modifier = Modifier.height(32.dp))
                CustomFilledButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    isLoading = viewState.isLoading,
                    text = stringResource(id = Strings.onboarding_sign_in),
                    onClick = viewModel::onSignInClicked,
                )

                Spacer(modifier = Modifier.height(8.dp))
                val uriHandler = LocalUriHandler.current
                CustomTextButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResource(id = Strings.login_forgot_password),
                    onClick = {
                        uriHandler.openUri("https://www.zotero.org/user/lostpassword?app=1")
                    },
                )

            }
        }
    }
}