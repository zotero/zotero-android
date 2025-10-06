package org.zotero.android.screens.creatoredit.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.creatoredit.CreatorEditViewModel
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3

@Composable
internal fun CreatorEditDeleteCreatorDialog(
    viewModel: CreatorEditViewModel
) {

    CustomAlertDialogM3(
        title = stringResource(id = Strings.warning),
        description = stringResource(
            id = Strings.creator_editor_delete_confirmation
        ),
        rightButtonColor = MaterialTheme.colorScheme.primary,
        rightButtonText = stringResource(id = Strings.cancel),
        leftButtonColor = MaterialTheme.colorScheme.error,
        leftButtonText = stringResource(id = Strings.delete),
        onLeftButtonClicked = viewModel::deleteCreator,
        onDismiss = viewModel::onDismissDeleteConformation
    )
}