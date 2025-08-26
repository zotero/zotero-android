package org.zotero.android.screens.settings.account.sections

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.settings.account.SettingsAccountViewModel
import org.zotero.android.screens.settings.account.SettingsAccountViewState
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.screens.settings.elements.NewSettingsItem
import org.zotero.android.screens.settings.elements.NewSettingsSectionTitle
import org.zotero.android.uicomponents.Strings

@Composable
internal fun SettingsAccountDataSyncSection(
    viewState: SettingsAccountViewState,
    viewModel: SettingsAccountViewModel
) {
    NewSettingsDivider()
    NewSettingsSectionTitle(titleId = Strings.settings_sync_data_syncing)

    NewSettingsItem(
        title = viewState.account,
        onItemTapped = {},
    )

    NewSettingsItem(
        textColor = MaterialTheme.colorScheme.error,
        title = stringResource(id = Strings.settings_logout),
        onItemTapped = viewModel::onShowSignOutDialog
    )
}