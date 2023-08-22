package org.zotero.android.architecture.navigation.tablet

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.navigation.CommonScreenDestinations
import org.zotero.android.architecture.navigation.DashboardTopLevelDialogs
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.screens.dashboard.BuildInfo
import org.zotero.android.screens.dashboard.DashboardViewModel
import org.zotero.android.screens.dashboard.DashboardViewState
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.systemui.SolidStatusBar
import org.zotero.android.uicomponents.theme.CustomTheme
import java.io.File

@Composable
internal fun DashboardRootTabletNavigationScreen(
    onPickFile: (callPoint: EventBusConstants.FileWasSelected.CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    onShowPdf: () -> Unit,
    toAddOrEditNote: () -> Unit,
    toZoteroWebViewScreen: (String) -> Unit,
    viewModel: DashboardViewModel,
) {
    val viewState by viewModel.viewStates.observeAsState(DashboardViewState())
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    SolidStatusBar()

    val rightPaneNavController = rememberAnimatedNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val rightPaneNavigation = remember(rightPaneNavController) {
        ZoteroNavigation(rightPaneNavController, dispatcher)
    }
    val navigateAndPopAllItemsScreen: () -> Unit = {
        rightPaneNavController.navigate(CommonScreenDestinations.ALL_ITEMS) {
            popUpTo(0)
        }
    }

    Box(
//        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.background(color = CustomTheme.colors.surface)) {
            BuildInfo()
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.35f)) {
                    TabletLeftPaneNavigation(
                        navigateAndPopAllItemsScreen = navigateAndPopAllItemsScreen,
                        onOpenWebpage = onOpenWebpage
                    )
                }
                CustomDivider(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                )
                Box(modifier = Modifier.weight(0.65f)) {
                    TabletRightPaneNavigation(
                        onPickFile = onPickFile,
                        onOpenFile = onOpenFile,
                        onShowPdf = onShowPdf,
                        onOpenWebpage = onOpenWebpage,
                        toAddOrEditNote = toAddOrEditNote,
                        toZoteroWebViewScreen = toZoteroWebViewScreen,
                        navController = rightPaneNavController,
                        navigation = rightPaneNavigation
                    )
                }

            }
        }
        DashboardTopLevelDialogs(viewState = viewState, viewModel = viewModel)
    }
}

