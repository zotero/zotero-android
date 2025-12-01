package org.zotero.android.screens.settings.account.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.zotero.android.screens.settings.account.SettingsAccountViewModel
import org.zotero.android.screens.settings.account.SettingsAccountViewState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.webdav.data.FileSyncType

@Composable
internal fun SettingsAccountWebDavOptionsDialog(
    viewModel: SettingsAccountViewModel,
    viewState: SettingsAccountViewState
) {
    Dialog(onDismissRequest = viewModel::dismissWebDavOptionsDialog) {
        val roundCornerShape = RoundedCornerShape(size = 30.dp)
        Column(
            Modifier
                .wrapContentSize()
                .clip(roundCornerShape)
                .background(MaterialTheme.colorScheme.surface)
                .selectableGroup()
        ) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                    Text(
                        text = "Sync files using",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
            )

            NewSettingsRadioButton(
                text = stringResource(Strings.file_syncing_zotero_option),
                isSelected = viewState.fileSyncType == FileSyncType.zotero,
                onOptionSelected = { viewModel.setFileSyncType(FileSyncType.zotero) }
            )
            NewSettingsRadioButton(
                text = stringResource(Strings.file_syncing_webdav_option),
                isSelected = viewState.fileSyncType == FileSyncType.webDav,
                onOptionSelected = { viewModel.setFileSyncType(FileSyncType.webDav) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }


    }
}

@Composable
private fun NewSettingsRadioButton(
    text: String,
    isSelected: Boolean,
    onOptionSelected: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .selectable(
                selected = isSelected,
                onClick = onOptionSelected,
                role = Role.RadioButton,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
