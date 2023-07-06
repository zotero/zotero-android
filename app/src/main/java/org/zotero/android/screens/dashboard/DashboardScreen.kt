package org.zotero.android.screens.dashboard

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.zotero.android.BuildConfig
import org.zotero.android.architecture.EventBusConstants.FileWasSelected.CallPoint
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.bottomsheet.LongPressBottomSheet
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.systemui.SolidStatusBar
import org.zotero.android.uicomponents.theme.CustomTheme
import java.io.File

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun DashboardScreen(
    onBack: () -> Unit,
    onPickFile: (callPoint: CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onShowPdf: () -> Unit,
    toAddOrEditNote: () -> Unit,
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
        Column(modifier = Modifier.background(color = CustomTheme.colors.surface)) {
            BuildInfo(layoutType)
            if (layoutType.isTablet()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(0.35f)) {
                        CollectionsAtRootNavigation(
                            rightPaneNavController = rightPaneNavController,
                            onOpenWebpage = onOpenWebpage
                        )
                    }
                    CustomDivider(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                    )
                    Box(modifier = Modifier.weight(0.65f)) {
                        FullScreenOrRightPaneNavigation(
                            onPickFile = onPickFile,
                            onOpenFile = onOpenFile,
                            onShowPdf = onShowPdf,
                            onOpenWebpage = onOpenWebpage,
                            toAddOrEditNote = toAddOrEditNote,
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
                    onShowPdf = onShowPdf,
                    toAddOrEditNote = toAddOrEditNote,
                )
            }
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


}

@Composable
private fun ColumnScope.BuildInfo(layoutType: CustomLayoutSize.LayoutType) {
    val buildType = if (BuildConfig.BUILD_TYPE == "eBeta") "Beta" else BuildConfig.BUILD_TYPE
    Text(
        modifier = Modifier.Companion.align(Alignment.CenterHorizontally),
        text = "Zotero ${buildType.replaceFirstChar(Char::titlecase)} (${BuildConfig.VERSION_NAME})",
        fontSize = layoutType.calculateBuildInfoTextSize(),
    )
    Spacer(modifier = Modifier.height(4.dp))
    CustomDivider()
}


