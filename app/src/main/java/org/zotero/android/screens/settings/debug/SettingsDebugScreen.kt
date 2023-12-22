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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.SettingsItem
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.quantityStringResource
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun SettingsDebugScreen(
    onBack: () -> Unit,
    toDebugLogScreen: () -> Unit,
    viewModel: SettingsDebugViewModel = hiltViewModel(),
) {
    val backgroundColor = CustomTheme.colors.zoteroItemDetailSectionBackground
    CustomThemeWithStatusAndNavBars(
        navBarBackgroundColor = backgroundColor,
    ) {
        val viewState by viewModel.viewStates.observeAsState(SettingsDebugViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                null -> Unit
                else -> {

                }
            }
        }
        CustomScaffold(
            backgroundColor = CustomTheme.colors.popupBackgroundContent,
            topBar = {
                SettingsDebugTopBar(
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
                SettingsSection {
                    if (viewState.isLogging) {
                        SettingsItem(
                            textColor = CustomTheme.colors.zoteroDefaultBlue,
                            title = stringResource(id = Strings.settings_cancel_logging),
                            onItemTapped = viewModel::cancelLogging
                        )
                        SettingsDivider()
                        SettingsItem(
                            textColor = CustomTheme.colors.zoteroDefaultBlue,
                            title = stringResource(id = Strings.settings_stop_logging),
                            onItemTapped = viewModel::stopLogging
                        )
                        SettingsDivider()
                        SettingsItem(
                            title = stringResource(id = Strings.settings_logging_desc1),
                            onItemTapped = {}
                        )
                        SettingsDivider()
                        SettingsItem(
                            title = stringResource(id = Strings.settings_logging_desc2),
                            onItemTapped = {}
                        )
                    } else {
                        SettingsItem(
                            textColor = CustomTheme.colors.zoteroDefaultBlue,
                            title = stringResource(id = Strings.settings_start_logging),
                            onItemTapped = viewModel::startLogging
                        )
                        SettingsDivider()
                        SettingsItem(
                            textColor = CustomTheme.colors.zoteroDefaultBlue,
                            title = stringResource(id = Strings.settings_start_logging_on_launch),
                            onItemTapped = viewModel::startLoggingOnNextAppLaunch
                        )
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
                if (viewState.isLogging) {
                    SettingsSection {
                        SettingsItem(
                            textColor = CustomTheme.colors.zoteroDefaultBlue,
                            title = stringResource(id = Strings.settings_view_output),
                            onItemTapped = toDebugLogScreen,
                            addNewScreenNavigationIndicator = true,
                        )
                        SettingsDivider()
                        SettingsItem(
                            textColor = CustomTheme.colors.zoteroDefaultBlue,
                            title = stringResource(id = Strings.settings_clear_output),
                            onItemTapped = viewModel::clearLogs
                        )
                        SettingsDivider()
                        SettingsItem(
                            title = quantityStringResource(
                                id = Plurals.settings_lines_logged,
                                viewState.numberOfLines
                            ),
                            onItemTapped = {},
                        )
                    }
                }
            }
        }
    }
}