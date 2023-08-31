package org.zotero.android.architecture.navigation.toolbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialog

@Composable
internal fun SyncToolbarDialogs(
    viewState: SyncToolbarViewState,
    viewModel: SyncToolbarViewModel
) {
    val dialogError = viewState.dialogError ?: return
    val alertMessage = syncToolbarAlertMessage(dialogError = dialogError, viewModel = viewModel)
    CustomAlertDialog(
        title = stringResource(id = Strings.error),
        description = alertMessage.first,
        primaryAction = CustomAlertDialog.ActionConfig(
            text = stringResource(id = Strings.ok),
            onClick = {
                viewModel.onDismissDialog()
            }
        ),
        onDismiss = viewModel::onDismissDialog
    )
}