package org.zotero.android.screens.settings.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.SidebarDivider
import org.zotero.android.screens.settings.SettingsItem
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CancelSaveTitleTopBar

@Composable
internal fun SettingsDebugScreen(
    onBack: () -> Unit,
    toDebugLogScreen: () -> Unit,
    viewModel: SettingsDebugViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
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
            TopBar(
                onBack = onBack,
            )
        },
    ) {
        CustomDivider()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = CustomTheme.colors.zoteroItemDetailSectionBackground)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            SettingsSection {
                if (viewState.isLogging) {
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = false,
                        textColor = CustomTheme.colors.zoteroBlueWithDarkMode,
                        title = stringResource(id = Strings.settings_debug_cancel_logging),
                        onItemTapped = viewModel::cancelLogging
                    )
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = false,
                        textColor = CustomTheme.colors.zoteroBlueWithDarkMode,
                        title = stringResource(id = Strings.settings_debug_stop_logging),
                        onItemTapped = viewModel::stopLogging
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        modifier = Modifier.padding(start = 12.dp),
                        text = stringResource(id = Strings.settings_debug_desc_1),
                        fontSize = layoutType.calculateItemsRowTextSize(),
                    )
                    SidebarDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        modifier = Modifier.padding(start = 12.dp),
                        text = stringResource(id = Strings.settings_debug_desc_2),
                        fontSize = layoutType.calculateItemsRowTextSize(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = false,
                        textColor = CustomTheme.colors.zoteroBlueWithDarkMode,
                        title = stringResource(id = Strings.settings_debug_start_logging),
                        onItemTapped = viewModel::startLogging
                    )
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = true,
                        textColor = CustomTheme.colors.zoteroBlueWithDarkMode,
                        title = stringResource(id = Strings.settings_debug_start_logging_on_next_launch),
                        onItemTapped = viewModel::startLoggingOnNextAppLaunch
                    )
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
            if (viewState.isLogging) {
                SettingsSection {
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = false,
                        textColor = CustomTheme.colors.zoteroBlueWithDarkMode,
                        title = stringResource(id = Strings.settings_debug_view_output),
                        onItemTapped = toDebugLogScreen
                    )
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = false,
                        textColor = CustomTheme.colors.zoteroBlueWithDarkMode,
                        title = stringResource(id = Strings.settings_debug_clear_output),
                        onItemTapped = viewModel::clearLogs
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        modifier = Modifier.padding(start = 12.dp),
                        text = stringResource(id = Strings.settings_debug_lines_logged, viewState.numberOfLines),
                        fontSize = layoutType.calculateItemsRowTextSize(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
) {
    CancelSaveTitleTopBar(
        title = stringResource(id = Strings.settings_debug_output_logging),
        onBack = onBack,
        backgroundColor = CustomTheme.colors.popupBackgroundContent,
    )
}