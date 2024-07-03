package org.zotero.android.screens.settings.account

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun SettingsAccountScreen(
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = backgroundColor)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(30.dp))
                SettingsAccountDataSyncSection(viewState, viewModel)

                //TODO uncomment for File Syncing functionality
//                Spacer(modifier = Modifier.height(30.dp))
//                SettingsAccountFileSyncingSection(viewState, viewModel)

                Spacer(modifier = Modifier.height(30.dp))
                SettingsAccountAccountSection(viewModel)
            }
        }
    }
}

