package org.zotero.android.screens.settings.account.sections

import androidx.compose.runtime.Composable
import org.zotero.android.screens.settings.account.SettingsAccountViewModel
import org.zotero.android.screens.settings.account.SettingsAccountViewState
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.screens.settings.elements.NewSettingsSectionTitle
import org.zotero.android.uicomponents.Strings
import org.zotero.android.webdav.data.FileSyncType

@Composable
internal fun SettingsAccountFileSyncingSection(
    viewState: SettingsAccountViewState,
    viewModel: SettingsAccountViewModel
) {
    NewSettingsDivider()
    NewSettingsSectionTitle(titleId = Strings.settings_sync_file_syncing)
    SettingsAccountFileSyncingSyncMethodChooserItem(viewModel, viewState)
    if (viewState.fileSyncType == FileSyncType.webDav) {
        SettingsAccountFileSyncingWebDavItems(viewModel, viewState)
    }
}