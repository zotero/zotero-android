package org.zotero.android.architecture.navigation.toolbar

import androidx.compose.runtime.Composable
import org.zotero.android.uicomponents.foundation.safeStringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3ActionConfig

@Composable
internal fun SyncToolbarDialogs(
    viewState: SyncToolbarViewState,
    viewModel: SyncToolbarViewModel
) {
    val alertMessage = viewState.dialogErrorMessage ?: return
    CustomAlertDialogM3(
        title = safeStringResource(id = Strings.error),
        description = alertMessage,
        confirmButton = CustomAlertDialogM3ActionConfig(
            text = safeStringResource(id = Strings.ok),
        ),
        onDismiss = viewModel::onDismissDialog
    )
}