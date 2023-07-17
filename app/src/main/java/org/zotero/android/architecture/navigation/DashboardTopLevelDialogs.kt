package org.zotero.android.architecture.navigation

import androidx.compose.runtime.Composable
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.dashboard.ChangedItemsDeletedAlert
import org.zotero.android.screens.dashboard.ConflictResolutionDialogs
import org.zotero.android.screens.dashboard.DashboardViewModel
import org.zotero.android.screens.dashboard.DashboardViewState
import org.zotero.android.uicomponents.bottomsheet.LongPressBottomSheet

@Composable
fun DashboardTopLevelDialogs(
    viewState: DashboardViewState,
    viewModel: DashboardViewModel,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
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
            keepGroupChanges = viewModel::keepGroupChanges,
            revertGroupFiles = viewModel::revertGroupFiles,
            skipGroup = viewModel::skipGroup,
        )
    }
    LongPressBottomSheet(
        layoutType = layoutType,
        longPressOptionsHolder = viewState.longPressOptionsHolder,
        onCollapse = viewModel::dismissBottomSheet,
        onOptionClick = viewModel::onLongPressOptionsItemSelected
    )
}