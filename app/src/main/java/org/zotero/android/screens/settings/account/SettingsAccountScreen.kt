package org.zotero.android.screens.settings.account

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialog
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun SettingsAccountScreen(
    navigateToSinglePickerScreen: () -> Unit,
    onBack: () -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    viewModel: SettingsAccountViewModel = hiltViewModel(),
) {
    val backgroundColor = CustomTheme.colors.zoteroItemDetailSectionBackground
    CustomThemeWithStatusAndNavBars(
        navBarBackgroundColor = backgroundColor,
    ) {
        val viewState by viewModel.viewStates.observeAsState(SettingsAccountViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                null -> Unit
                is SettingsAccountViewEffect.OnBack -> {
                    onBack()
                }

                is SettingsAccountViewEffect.OpenWebpage -> {
                    onOpenWebpage(consumedEffect.uri)
                }

                SettingsAccountViewEffect.NavigateToSinglePickerScreen -> {
                    navigateToSinglePickerScreen()
                }
            }
        }
        CustomScaffold(
            backgroundColor = CustomTheme.colors.popupBackgroundContent,
            topBar = {
                SettingsAccountTopBar(
                    onBack = onBack,
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = backgroundColor)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                    SettingsAccountDataSyncSection(viewState, viewModel)
                }
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                    SettingsAccountFileSyncingSection(viewState, viewModel)
                }
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                    SettingsAccountAccountSection(viewModel)
                }
            }
            val createWebDavDirectoryDialogData = viewState.createWebDavDirectoryDialogData
            if (createWebDavDirectoryDialogData != null) {
                DirectoryNotFoundDialog(url = createWebDavDirectoryDialogData.url,
                    onCancel = {
                        viewModel.onDismissCreateDirectoryDialog(createWebDavDirectoryDialogData.error)
                    },
                    onCreate = viewModel::onCreateWebDavDirectory
                )
            }

            val shouldShowSignOutDialog = viewState.shouldShowSignOutDialog
            if (shouldShowSignOutDialog) {
                SignOutDialog(
                    onCancel = viewModel::onDismissSignOutDialog,
                    onSignOut = viewModel::onSignOut
                )
            }

        }
    }
}

@Composable
private fun DirectoryNotFoundDialog (
    url: String,
    onCreate: () -> Unit,
    onCancel: () -> Unit,
) {
    CustomAlertDialog(
        title = stringResource(id = Strings.settings_sync_directory_not_found_title),
        description = stringResource(
            id = Strings.settings_sync_directory_not_found_message, url
        ),
        descriptionTextColor = CustomTheme.colors.primaryContent,
        primaryAction = CustomAlertDialog.ActionConfig(
            text = stringResource(id = Strings.cancel),
        ),
        secondaryAction = CustomAlertDialog.ActionConfig(
            text = stringResource(id = Strings.create),
            onClick = onCreate
        ),
        dismissOnClickOutside = false,
        onDismiss = onCancel
    )
}

@Composable
private fun SignOutDialog (
    onSignOut: () -> Unit,
    onCancel: () -> Unit,
) {
    CustomAlertDialog(
        title = stringResource(id = Strings.warning),
        description = stringResource(
            id = Strings.settings_logout_warning
        ),
        descriptionTextColor = CustomTheme.colors.primaryContent,
        primaryAction = CustomAlertDialog.ActionConfig(
            text = stringResource(id = Strings.no),
        ),
        secondaryAction = CustomAlertDialog.ActionConfig(
            text = stringResource(id = Strings.yes),
            onClick = onSignOut
        ),
        onDismiss = onCancel
    )
}
