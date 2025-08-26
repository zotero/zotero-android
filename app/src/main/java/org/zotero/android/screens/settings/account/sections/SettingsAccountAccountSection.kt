package org.zotero.android.screens.settings.account.sections

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.settings.account.SettingsAccountViewModel
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.screens.settings.elements.NewSettingsItem
import org.zotero.android.screens.settings.elements.NewSettingsSectionTitle
import org.zotero.android.uicomponents.Strings

@Composable
internal fun SettingsAccountAccountSection(viewModel: SettingsAccountViewModel) {
    NewSettingsDivider()
    NewSettingsSectionTitle(
        titleId = Strings.settings_sync_title
    )
    NewSettingsItem(
        textColor = MaterialTheme.colorScheme.primary,
        title = stringResource(id = Strings.settings_sync_manage_account),
        onItemTapped = viewModel::openManageAccount
    )
    NewSettingsItem(
        textColor = MaterialTheme.colorScheme.error,
        title = stringResource(id = Strings.settings_sync_delete_account),
        onItemTapped = viewModel::openDeleteAccount
    )
}