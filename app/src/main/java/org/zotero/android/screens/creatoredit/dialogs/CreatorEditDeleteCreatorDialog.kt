package org.zotero.android.screens.creatoredit.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.creatoredit.CreatorEditViewModel
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3ActionConfig

@Composable
internal fun CreatorEditDeleteCreatorDialog(
    viewModel: CreatorEditViewModel
) {

    CustomAlertDialogM3(
        title = stringResource(id = Strings.warning),
        description = stringResource(
            id = Strings.creator_editor_delete_confirmation
        ),
        dismissButton = CustomAlertDialogM3ActionConfig(
            text = stringResource(id = Strings.cancel),
        ),
        confirmButton = CustomAlertDialogM3ActionConfig(
            text = stringResource(id = Strings.delete),
            textColor = MaterialTheme.colorScheme.error,
            onClick = viewModel::deleteCreator
        ),
        onDismiss = viewModel::onDismissDeleteConformation
    )
}