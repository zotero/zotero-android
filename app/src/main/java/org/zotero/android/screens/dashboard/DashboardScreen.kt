package org.zotero.android.screens.dashboard

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.architecture.EventBusConstants.FileWasSelected.CallPoint
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.sync.Library
import org.zotero.android.uicomponents.bottomsheet.LongPressBottomSheet
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.systemui.SolidStatusBar
import java.io.File

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun DashboardScreen(
    onBack: () -> Unit,
    onPickFile: (callPoint: CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onShowPdf: (file: File, key: String, library: Library) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    viewModel: DashboardViewModel,
) {
    val viewState by viewModel.viewStates.observeAsState(DashboardViewState())
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    SolidStatusBar()

    val rightPaneNavController = rememberAnimatedNavController()
    val rightPaneDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val rightPaneNavigation = remember(rightPaneNavController) {
        Navigation(rightPaneNavController, rightPaneDispatcher)
    }
    val layoutType = CustomLayoutSize.calculateLayoutType()
    Box {
        if (layoutType.isTablet()) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.3f)) {
                    CollectionsAtRootNavigation(rightPaneNavController)
                }
                CustomDivider(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                )
                Box(modifier = Modifier.weight(0.7f)) {
                    FullScreenOrRightPaneNavigation(
                        onPickFile = onPickFile,
                        onOpenFile = onOpenFile,
                        onShowPdf = onShowPdf,
                        onOpenWebpage = onOpenWebpage,
                        navController = rightPaneNavController,
                        navigation = rightPaneNavigation
                    )
                }

            }
        } else {
            FullScreenOrRightPaneNavigation(
                navController = rightPaneNavController,
                navigation = rightPaneNavigation,
                onPickFile = onPickFile,
                onOpenWebpage = onOpenWebpage,
                onOpenFile = onOpenFile,
                onShowPdf = onShowPdf
            )
        }
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
            )
        }
        LongPressBottomSheet(
            layoutType = layoutType,
            longPressOptionsHolder = viewState.longPressOptionsHolder,
            onCollapse = viewModel::dismissBottomSheet,
            onOptionClick = viewModel::onLongPressOptionsItemSelected
        )
    }

}


