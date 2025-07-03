package org.zotero.android.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings


@Composable
internal fun SettingsSupportAndPrivacySection(viewModel: SettingsViewModel) {
    SettingsSection {
        SettingsItem(
            title = stringResource(id = Strings.support_feedback),
            onItemTapped = viewModel::openSupportAndFeedback
        )
        SettingsDivider()
        SettingsItem(
            title = stringResource(id = Strings.privacy_policy),
            onItemTapped = viewModel::openPrivacyPolicy
        )
    }
}

@Composable
internal fun SettingsDebugSection(toDebugScreen: () -> Unit, toCiteScreen: () -> Unit) {
    SettingsSection {
        SettingsItem(
            title = stringResource(id = Strings.settings_cite_title),
            onItemTapped = toCiteScreen,
            addNewScreenNavigationIndicator = true,
        )
        SettingsDivider()
        SettingsItem(
            title = stringResource(id = Strings.settings_debug),
            onItemTapped = toDebugScreen,
            addNewScreenNavigationIndicator = true,
        )
    }
}

@Composable
internal fun SettingsSyncAccountSection(toAccountScreen: () -> Unit) {
    SettingsSection {
        SettingsItem(
            title = stringResource(id = Strings.settings_sync_account),
            onItemTapped = toAccountScreen,
            addNewScreenNavigationIndicator = true,
        )
    }
}