package org.zotero.android.screens.settings.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun SettingsDebugScreen(
    onBack: () -> Unit,
    toDebugLogScreen: () -> Unit,
    viewModel: SettingsDebugViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(SettingsDebugViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                else -> {

                }
            }
        }
        CustomScaffoldM3(
            topBar = {
                SettingsDebugTopBar(
                    onBack = onBack,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (viewState.isLogging) {
                    SettingsDebugCancelLoggingSection(viewModel)
                    SettingsDebugStopLoggingSection(viewModel)
                    SettingsDebugLoggingDescriptionSection()
                } else {
                    SettingsDebugStartLoggingSection(viewModel)
                    SettingsDebugStartLoggingOnLaunchSection(viewModel)
                }
                if (viewState.isLogging) {
                    NewSettingsDivider()
                    SettingsDebugViewOutputSection(toDebugLogScreen)
                    SettingsDebugClearOutputSection(viewModel)
                    SettingsDebugLinesLoggedSection(viewState)
                }
            }

        }
    }
}




