package org.zotero.android.screens.settings.debug

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.settings.elements.NewSettingsItem
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.quantityStringResource

@Composable
internal fun SettingsDebugCancelLoggingSection(viewModel: SettingsDebugViewModel) {
    NewSettingsItem(
        textColor = MaterialTheme.colorScheme.primary,
        title = stringResource(id = Strings.settings_cancel_logging),
        onItemTapped = viewModel::cancelLogging
    )
}

@Composable
internal fun SettingsDebugStopLoggingSection(viewModel: SettingsDebugViewModel) {
    NewSettingsItem(
        textColor = MaterialTheme.colorScheme.primary,
        title = stringResource(id = Strings.settings_stop_logging),
        onItemTapped = viewModel::stopLogging
    )
}


@Composable
internal fun SettingsDebugLinesLoggedSection(viewState: SettingsDebugViewState) {
    NewSettingsItem(
        title = quantityStringResource(
            id = Plurals.settings_lines_logged,
            viewState.numberOfLines
        ),
        onItemTapped = {},
    )
}

@Composable
internal fun SettingsDebugClearOutputSection(viewModel: SettingsDebugViewModel) {
    NewSettingsItem(
        textColor = MaterialTheme.colorScheme.primary,
        title = stringResource(id = Strings.settings_clear_output),
        onItemTapped = viewModel::clearLogs
    )
}

@Composable
internal fun SettingsDebugViewOutputSection(toDebugLogScreen: () -> Unit) {
    NewSettingsItem(
        textColor = MaterialTheme.colorScheme.primary,
        title = stringResource(id = Strings.settings_view_output),
        onItemTapped = toDebugLogScreen,
    )
}

@Composable
internal fun SettingsDebugStartLoggingOnLaunchSection(viewModel: SettingsDebugViewModel) {
    NewSettingsItem(
        textColor = MaterialTheme.colorScheme.primary,
        title = stringResource(id = Strings.settings_start_logging_on_launch),
        onItemTapped = viewModel::startLoggingOnNextAppLaunch
    )
}

@Composable
internal fun SettingsDebugStartLoggingSection(viewModel: SettingsDebugViewModel) {
    NewSettingsItem(
        textColor = MaterialTheme.colorScheme.primary,
        title = stringResource(id = Strings.settings_start_logging),
        onItemTapped = viewModel::startLogging
    )
}

@Composable
internal fun SettingsDebugLoggingDescriptionSection() {
    SettingsDebugDescriptionItem(
        title = stringResource(id = Strings.settings_logging_desc1),
        onItemTapped = {}
    )
    SettingsDebugDescriptionItem(
        title = stringResource(id = Strings.settings_logging_desc2),
        onItemTapped = {}
    )
}