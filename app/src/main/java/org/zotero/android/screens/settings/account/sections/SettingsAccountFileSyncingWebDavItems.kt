package org.zotero.android.screens.settings.account.sections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.api.network.CustomResult
import org.zotero.android.screens.settings.account.SettingsAccountViewModel
import org.zotero.android.screens.settings.account.SettingsAccountViewState
import org.zotero.android.screens.settings.elements.NewSettingsDivider

@Composable
internal fun SettingsAccountFileSyncingWebDavItems(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    SettingsAccountFileSyncingWebDavUrlItem(viewModel = viewModel, viewState = viewState)
    Spacer(modifier = Modifier.height(12.dp))
    SettingsAccountFileSyncingUsernameItem(viewModel = viewModel, viewState = viewState)
    Spacer(modifier = Modifier.height(12.dp))
    SettingsAccountFileSyncingPasswordItem(viewModel = viewModel, viewState = viewState)
    
    // Warning for HTTP on local network
    if (viewState.scheme == org.zotero.android.webdav.data.WebDavScheme.http) {
        Spacer(modifier = Modifier.height(12.dp))
        org.zotero.android.uicomponents.NewSettingsText(
            text = androidx.compose.ui.res.stringResource(id = org.zotero.android.uicomponents.Strings.settings_webdav_http_public_network_warning),
            color = androidx.compose.material3.MaterialTheme.colorScheme.error
        )
    }

    if (viewState.isVerifyingWebDav) {
        SettingsAccountFileSyncingVerificationInProgressItem(
            viewModel = viewModel,
            viewState = viewState
        )
    } else {
        SettingsAccountFileSyncingVerifyServerItem(viewModel = viewModel, viewState = viewState)
    }

    val webDavVerificationResult = viewState.webDavVerificationResult
    if (webDavVerificationResult is CustomResult.GeneralError) {
        NewSettingsDivider()
        SettingsAccountFileSyncingErrorMessageItem(webDavVerificationResult)
    }
}
