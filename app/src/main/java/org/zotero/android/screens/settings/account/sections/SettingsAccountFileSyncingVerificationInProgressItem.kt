package org.zotero.android.screens.settings.account.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.settings.account.SettingsAccountViewModel
import org.zotero.android.screens.settings.account.SettingsAccountViewState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun SettingsAccountFileSyncingVerificationInProgressItem(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 16.dp)
                .align(Alignment.CenterStart)
                .size(24.dp)
        )
        Text(
            modifier = Modifier
                .padding(end = 16.dp)
                .align(Alignment.CenterEnd)
                .safeClickable(
                    onClick = viewModel::cancelVerification,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                ),
            text = stringResource(id = Strings.cancel),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
