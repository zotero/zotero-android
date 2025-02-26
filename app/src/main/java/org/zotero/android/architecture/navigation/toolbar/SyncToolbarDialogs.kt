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
    val alertMessage = viewState.dialogErrorMessage ?: return
    CustomAlertDialog(
        title = stringResource(id = Strings.error),
        description = alertMessage,
        primaryAction = CustomAlertDialog.ActionConfig(
            text = stringResource(id = Strings.ok),
            onClick = {
                viewModel.onDismissDialog()
            }
        ),
        onDismiss = viewModel::onDismissDialog
    )
}