package org.zotero.android.screens.settings.account.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.settings.account.SettingsAccountViewModel
import org.zotero.android.screens.settings.account.SettingsAccountViewState
import org.zotero.android.screens.settings.account.dialogs.SettingsAccountWebDavOptionsDialog
import org.zotero.android.screens.settings.elements.NewSettingsItemWithDescription
import org.zotero.android.uicomponents.Strings
import org.zotero.android.webdav.data.FileSyncType

@Composable
internal fun SettingsAccountFileSyncingSyncMethodChooserItem(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    if (viewState.showWebDavOptionsDialog) {
        SettingsAccountWebDavOptionsDialog(viewModel, viewState)
    }
    NewSettingsItemWithDescription(
        title = stringResource(id = Strings.settings_sync_file_syncing_type_message),
        description = stringResource(
            id = if (viewState.fileSyncType == FileSyncType.zotero) {
                Strings.file_syncing_zotero_option
            } else {
                Strings.file_syncing_webdav_option
            }
        ),
        onItemTapped = viewModel::showWebDavOptionsPopup
    )
}
