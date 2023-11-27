package org.zotero.android.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.login.LoginViewEffect.NavigateBack
import org.zotero.android.screens.login.LoginViewEffect.NavigateToDashboard
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.button.PrimaryButton
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun LoginScreen(
    onBack: () -> Unit,
    navigateToDashboard: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    CustomThemeWithStatusAndNavBars {
        val layoutType = CustomLayoutSize.calculateLayoutType()
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
        CustomScaffold(
            topBar = {
                LoginTopBar(
                    onCancelClicked = onBack,
                )
            },
            snackbarMessage = viewState.snackbarMessage,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = CustomTheme.colors.surface),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 430.dp)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    val focusManager = LocalFocusManager.current
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    val moveFocusDownAction = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                    CustomTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = viewState.username,
                        hint = stringResource(id = Strings.login_username),
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
                        onEnterOrTab = { moveFocusDownAction() }
                    )
                    CustomDivider(modifier = Modifier.padding(vertical = 16.dp))
                    CustomTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = viewState.password,
                        hint = stringResource(id = Strings.login_password),
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
                        onEnterOrTab = { viewModel.onSignInClicked() }
                    )
                    CustomDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    PrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResource(id = Strings.onboarding_sign_in),
                        onClick = viewModel::onSignInClicked,
                        isLoading = viewState.isLoading
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    val uriHandler = LocalUriHandler.current
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .safeClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    uriHandler.openUri("https://www.zotero.org/user/lostpassword?app=1")
                                }
                            ),
                        text = stringResource(id = Strings.login_forgot_password),
                        color = CustomTheme.colors.zoteroBlueWithDarkMode,
                        style = CustomTheme.typography.default,
                        fontSize = layoutType.calculateTextSize(),
                    )
                }
            }
        }
    }
}