package org.zotero.android.screens.settings.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.SettingsItem
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.screens.settings.SettingsSectionTitle
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme


@Composable
internal fun SettingsAccountDataSyncSection(
    viewState: SettingsAccountViewState,
    viewModel: SettingsAccountViewModel
) {
    SettingsSectionTitle(titleId = Strings.settings_sync_data_syncing)
    SettingsSection {
        SettingsItem(
            title = viewState.account,
            onItemTapped = {}
        )
        SettingsDivider()
        SettingsItem(
            textColor = CustomPalette.ErrorRed,
            title = stringResource(id = Strings.settings_logout),
            onItemTapped = viewModel::onShowSignOutDialog
        )
    }
}

@Composable
internal fun SettingsAccountAccountSection(viewModel: SettingsAccountViewModel) {
    SettingsSectionTitle(
        titleId = Strings.settings_sync_title
    )
    SettingsSection {
        SettingsItem(
            textColor = CustomTheme.colors.zoteroDefaultBlue,
            title = stringResource(id = Strings.settings_sync_manage_account),
            onItemTapped = viewModel::openManageAccount
        )
        SettingsDivider()
        SettingsItem(
            textColor = CustomPalette.ErrorRed,
            title = stringResource(id = Strings.settings_sync_delete_account),
            onItemTapped = viewModel::openDeleteAccount
        )
    }
}