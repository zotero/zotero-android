package org.zotero.android.screens.settings.account

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.SettingsItem
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.screens.settings.SettingsSectionTitle
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.webdav.data.FileSyncType




@Composable
internal fun SettingsAccountDataSyncSection(
    viewState: SettingsAccountViewState,
    viewModel: SettingsAccountViewModel
) {
    SettingsSectionTitle(titleId = Strings.settings_data_sync)
    SettingsSection {
        SettingsItem(
            title = viewState.account,
            onItemTapped = {}
        )
        SettingsDivider()
        SettingsItem(
            textColor = CustomPalette.ErrorRed,
            title = stringResource(id = Strings.settings_logout),
            onItemTapped = viewModel::onSignOut
        )
    }
}

@Composable
internal fun SettingsAccountAccountSection(viewModel: SettingsAccountViewModel) {
    SettingsSectionTitle(
        titleId = Strings.settings_account_caps
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