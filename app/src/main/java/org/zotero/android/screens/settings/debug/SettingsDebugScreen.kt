package org.zotero.android.screens.settings.debug

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
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun SettingsDebugScreen(
    onBack: () -> Unit,
    toDebugLogScreen: () -> Unit,
    viewModel: SettingsDebugViewModel = hiltViewModel(),
) {
    CustomThemeWithStatusAndNavBars {
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
        CustomScaffold(
            topBarColor = CustomTheme.colors.surface,
            bottomBarColor = CustomTheme.colors.zoteroItemDetailSectionBackground,
            topBar = {
                SettingsDebugTopBar(
                    onBack = onBack,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = CustomTheme.colors.zoteroItemDetailSectionBackground)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(30.dp))
                SettingsSection {
                    if (viewState.isLogging) {
                        SettingsDebugCancelLoggingSection(viewModel)
                        SettingsDivider()
                        SettingsDebugStopLoggingSection(viewModel)
                        SettingsDivider()
                        SettingsDebugLoggingDescriptionSection()
                    } else {
                        SettingsDebugStartLoggingSection(viewModel)
                        SettingsDivider()
                        SettingsDebugStartLoggingOnLaunchSection(viewModel)
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
                if (viewState.isLogging) {
                    SettingsSection {
                        SettingsDebugViewOutputSection(toDebugLogScreen)
                        SettingsDivider()
                        SettingsDebugClearOutputSection(viewModel)
                        SettingsDivider()
                        SettingsDebugLinesLoggedSection(viewState)
                    }
                }
            }
        }
    }
}




