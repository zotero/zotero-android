package org.zotero.android.screens.settings.pageturning

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun SettingsPageTurningScreen(
    onBack: () -> Unit,
    viewModel: SettingsPageTurningViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(SettingsPageTurningViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                null -> Unit
                is SettingsPageTurningViewEffect.OnBack -> {
                    onBack()
                }

            }
        }
        CustomScaffoldM3(
            topBar = {
                SettingsPageTurningTopBar(
                    onBack = onBack,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                SettingsPageTurningSections(
                    buttonPageTurning = viewState.pageTurning,
                    onButtonPageTurningSwitchTapped = viewModel::onButtonPageTurningSwitchTapped,
                    buttonKeepZoom = viewState.keepZoom,
                    onButtonKeepZoomSwitchTapped = viewModel::onButtonKeepZoomSwitchTapped,
                )
            }
        }
    }
}
