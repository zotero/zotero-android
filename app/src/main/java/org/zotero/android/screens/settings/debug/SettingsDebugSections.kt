package org.zotero.android.screens.settings.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.SettingsItem
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.quantityStringResource
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun SettingsDebugCancelLoggingSection(viewModel: SettingsDebugViewModel) {
    SettingsItem(
        textColor = CustomTheme.colors.zoteroDefaultBlue,
        title = stringResource(id = Strings.settings_cancel_logging),
        onItemTapped = viewModel::cancelLogging
    )
}

@Composable
internal fun SettingsDebugStopLoggingSection(viewModel: SettingsDebugViewModel) {
    SettingsItem(
        textColor = CustomTheme.colors.zoteroDefaultBlue,
        title = stringResource(id = Strings.settings_stop_logging),
        onItemTapped = viewModel::stopLogging
    )
}


@Composable
internal fun SettingsDebugLinesLoggedSection(viewState: SettingsDebugViewState) {
    SettingsItem(
        title = quantityStringResource(
            id = Plurals.settings_lines_logged,
            viewState.numberOfLines
        ),
        onItemTapped = {},
    )
}

@Composable
internal fun SettingsDebugClearOutputSection(viewModel: SettingsDebugViewModel) {
    SettingsItem(
        textColor = CustomTheme.colors.zoteroDefaultBlue,
        title = stringResource(id = Strings.settings_clear_output),
        onItemTapped = viewModel::clearLogs
    )
}

@Composable
internal fun SettingsDebugViewOutputSection(toDebugLogScreen: () -> Unit) {
    SettingsItem(
        textColor = CustomTheme.colors.zoteroDefaultBlue,
        title = stringResource(id = Strings.settings_view_output),
        onItemTapped = toDebugLogScreen,
        addNewScreenNavigationIndicator = true,
    )
}

@Composable
internal fun SettingsDebugStartLoggingOnLaunchSection(viewModel: SettingsDebugViewModel) {
    SettingsItem(
        textColor = CustomTheme.colors.zoteroDefaultBlue,
        title = stringResource(id = Strings.settings_start_logging_on_launch),
        onItemTapped = viewModel::startLoggingOnNextAppLaunch
    )
}

@Composable
internal fun SettingsDebugStartLoggingSection(viewModel: SettingsDebugViewModel) {
    SettingsItem(
        textColor = CustomTheme.colors.zoteroDefaultBlue,
        title = stringResource(id = Strings.settings_start_logging),
        onItemTapped = viewModel::startLogging
    )
}

@Composable
internal fun SettingsDebugLoggingDescriptionSection() {
    SettingsItem(
        title = stringResource(id = Strings.settings_logging_desc1),
        onItemTapped = {}
    )
    SettingsDivider()
    SettingsItem(
        title = stringResource(id = Strings.settings_logging_desc2),
        onItemTapped = {}
    )
}