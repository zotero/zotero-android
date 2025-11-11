package org.zotero.android.screens.settings.account

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.settings.account.dialogs.SettingsDirectoryNotFoundDialog
import org.zotero.android.screens.settings.account.dialogs.SettingsSignOutDialog
import org.zotero.android.screens.settings.account.sections.SettingsAccountAccountSection
import org.zotero.android.screens.settings.account.sections.SettingsAccountDataSyncSection
import org.zotero.android.screens.settings.account.sections.SettingsAccountFileSyncingSection
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun SettingsAccountScreen(
    navigateToSinglePickerScreen: () -> Unit,
    onBack: () -> Unit,
    onOpenWebpage: (url: String) -> Unit,
    viewModel: SettingsAccountViewModel = hiltViewModel(),
) {
    AppThemeM3 {
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
                    onOpenWebpage(consumedEffect.url)
                }

                SettingsAccountViewEffect.NavigateToSinglePickerScreen -> {
                    navigateToSinglePickerScreen()
                }
            }
        }
        CustomScaffoldM3(
            topBar = {
                SettingsAccountTopBar(
                    onBack = onBack,
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                item {
                    SettingsAccountDataSyncSection(viewState, viewModel)
                }
                item {
                    SettingsAccountFileSyncingSection(viewState, viewModel)
                }
                item {
                    SettingsAccountAccountSection(viewModel)
                }

            }
            val createWebDavDirectoryDialogData = viewState.createWebDavDirectoryDialogData
            if (createWebDavDirectoryDialogData != null) {
                SettingsDirectoryNotFoundDialog(
                    url = createWebDavDirectoryDialogData.url,
                    onCancel = {
                        viewModel.onDismissCreateDirectoryDialog(createWebDavDirectoryDialogData.error)
                    },
                    onCreate = viewModel::onCreateWebDavDirectory
                )
            }

            val shouldShowSignOutDialog = viewState.shouldShowSignOutDialog
            if (shouldShowSignOutDialog) {
                SettingsSignOutDialog(
                    onCancel = viewModel::onDismissSignOutDialog,
                    onSignOut = viewModel::onSignOut
                )
            }

        }
    }
}

