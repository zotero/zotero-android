package org.zotero.android.architecture.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.dashboard.ChangedItemsDeletedAlert
import org.zotero.android.screens.dashboard.ConflictResolutionDialogs
import org.zotero.android.screens.dashboard.DashboardViewModel
import org.zotero.android.screens.dashboard.DashboardViewState
import org.zotero.android.screens.dashboard.DebugLoggingDialogs
import org.zotero.android.uicomponents.bottomsheet.LongPressBottomSheet
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun BoxScope.DashboardTopLevelDialogs(
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

    LongPressBottomSheet(
        layoutType = layoutType,
        longPressOptionsHolder = viewState.longPressOptionsHolder,
        onCollapse = viewModel::dismissBottomSheet,
        onOptionClick = viewModel::onLongPressOptionsItemSelected
    )
    DebugStopButton(isVisible = viewState.showDebugWindow, onClick = viewModel::onDebugStop)
}

@Composable
private fun BoxScope.DebugStopButton(isVisible: Boolean, onClick: () -> Unit) {
    if (isVisible) {
        val color = CustomTheme.colors.zoteroBlueWithDarkMode
        Canvas(modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(50.dp)
            .size(60.dp)
            .safeClickable(onClick = onClick), onDraw = {
            drawCircle(color = color)
            drawRect(
                topLeft = Offset(x = 20.dp.toPx(), y = 20.dp.toPx()),
                size = Size(width = 20.dp.toPx(), height = 20.dp.toPx()),
                color = Color.White
            )
        })
    }
}