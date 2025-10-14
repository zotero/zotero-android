package org.zotero.android.screens.settings.account.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.api.network.CustomResult
import org.zotero.android.screens.settings.account.SettingsAccountViewModel
import org.zotero.android.screens.settings.account.SettingsAccountViewState
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun SettingsAccountFileSyncingVerifyServerItem(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
//            .background(CustomTheme.colors.surface)
    ) {

        val containerColor = if (viewState.canVerifyServer) {
            MaterialTheme.colorScheme.primary
        } else {
            CustomTheme.colors.disabledContent
        }
        FilledTonalButton(
            modifier = Modifier
                .padding(start = 16.dp)
                .align(Alignment.CenterStart),
            onClick = viewModel::verify,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = containerColor),
        ) {
            Text(
                text = stringResource(Strings.settings_sync_verify),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
        if (viewState.webDavVerificationResult is CustomResult.GeneralSuccess) {
            Row(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .align(Alignment.CenterEnd),
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                    text = stringResource(id = Strings.settings_sync_verified),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                    painter = painterResource(Drawables.check_24px),
                    contentDescription = null,
                    tint = CustomPalette.Green,
                )
            }

        }
    }
}
