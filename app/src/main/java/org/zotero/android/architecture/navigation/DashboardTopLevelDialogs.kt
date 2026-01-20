package org.zotero.android.architecture.navigation

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.dashboard.ChangedItemsDeletedAlert
import org.zotero.android.screens.dashboard.ConflictResolutionDialogs
import org.zotero.android.screens.dashboard.CrashLoggingDialogs
import org.zotero.android.screens.dashboard.DashboardViewModel
import org.zotero.android.screens.dashboard.DashboardViewState
import org.zotero.android.screens.dashboard.DebugLoggingDialogs
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.bottomsheet.LongPressBottomSheetM3
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3ActionConfig
import org.zotero.android.uicomponents.theme.CustomPalette

@Composable
fun BoxScope.DashboardTopLevelDialogs(
    viewState: DashboardViewState,
    viewModel: DashboardViewModel,
) {
    val changedItemsDeletedAlert = viewState.changedItemsDeletedAlertQueue.firstOrNull()
    if (changedItemsDeletedAlert != null) {
        ChangedItemsDeletedAlert(
            conflictDialogData = changedItemsDeletedAlert,
            deleteRemovedItemsWithLocalChanges = viewModel::deleteRemovedItemsWithLocalChanges,
            restoreRemovedItemsWithLocalChanges = viewModel::restoreRemovedItemsWithLocalChanges
        )
    }
    val conflictDialogData = viewState.conflictDialog
    if (conflictDialogData != null) {
        ConflictResolutionDialogs(
            conflictDialogData = conflictDialogData,
            onDismissDialog = viewModel::onDismissConflictDialog,
            deleteGroup = viewModel::deleteGroup,
            markGroupAsLocalOnly = viewModel::markGroupAsLocalOnly,
            revertGroupChanges = viewModel::revertGroupChanges,
            revertGroupFiles = viewModel::revertGroupFiles,
            skipGroup = viewModel::skipGroup,
        )
    }
    val debugLoggingDialogData = viewState.debugLoggingDialogData
    if (debugLoggingDialogData != null) {
        DebugLoggingDialogs(
            dialogData = debugLoggingDialogData,
            onDismissDialog = viewModel::onDismissDebugLoggingDialog,
            onContentReadingRetry = viewModel::onContentReadingRetry,
            onContentReadingOk = viewModel::onContentReadingOk,
            onUploadRetry = viewModel::onUploadRetry,
            onUploadOk = viewModel::onUploadOk,
            onShareCopy = viewModel::onShareCopy
        )
    }

    val crashReportIdDialogData = viewState.crashReportIdDialogData
    if (crashReportIdDialogData != null) {
        CrashLoggingDialogs(
            dialogData = crashReportIdDialogData,
            onDismissDialog = viewModel::onDismissCrashLoggingDialog,
            onShareCopy = viewModel::onShareCopy
        )
    }

    val deleteGroupDialogData = viewState.deleteGroupDialogData
    if (deleteGroupDialogData != null) {
        CustomAlertDialogM3(
            title = stringResource(id = Strings.delete),
            description = stringResource(
                id = Strings.libraries_delete_question, deleteGroupDialogData.name
            ),
            dismissButton = CustomAlertDialogM3ActionConfig(
                text = stringResource(id = Strings.no),
            ),
            confirmButton = CustomAlertDialogM3ActionConfig(
                text = stringResource(id = Strings.yes),
                textColor = CustomPalette.ErrorRed,
                onClick = { viewModel.deleteNonLocalGroup(deleteGroupDialogData.id) }
            ),
            dismissOnClickOutside = false,
            onDismiss = viewModel::onDismissDeleteGroupDialog
        )
    }

    LongPressBottomSheetM3(
        longPressOptionsHolder = viewState.longPressOptionsHolder,
        onCollapse = viewModel::dismissBottomSheet,
        onOptionClick = viewModel::onLongPressOptionsItemSelected
    )
    DebugStopButton(isVisible = viewState.showDebugWindow, onClick = viewModel::onDebugStop)
}
