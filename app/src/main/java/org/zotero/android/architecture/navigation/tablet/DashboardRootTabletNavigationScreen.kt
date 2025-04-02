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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.Consumable
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.navigation.CommonScreenDestinations
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.screens.dashboard.DashboardViewEffect
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import java.io.File

@Composable
internal fun DashboardRootTabletNavigationScreen(
    collectionDefaultValue: String,
    onPickFile: (callPoint: EventBusConstants.FileWasSelected.CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    onShowPdf: (String) -> Unit,
    toAddOrEditNote: (String) -> Unit,
    toZoteroWebViewScreen: (String) -> Unit,
    navigateToRetrieveMetadata: (params: String) -> Unit,
    viewEffect: Consumable<DashboardViewEffect>?,
) {

    val rightPaneNavController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val rightPaneNavigation = remember(rightPaneNavController) {
        ZoteroNavigation(rightPaneNavController, dispatcher)
    }
    val navigateAndPopAllItemsScreen: (String) -> Unit = {
        rightPaneNavController.navigate(CommonScreenDestinations.ALL_ITEMS) {
            popUpTo(0)
        }
    }

    Column(modifier = Modifier.background(color = CustomTheme.colors.surface)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(0.35f)) {
                TabletLeftPaneNavigation(
                    collectionDefaultValue = collectionDefaultValue,
                    viewEffect = viewEffect,
                    navigateAndPopAllItemsScreen = navigateAndPopAllItemsScreen,
                    onOpenWebpage = onOpenWebpage
                )
            }
            NewDivider(
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
                    navigateToRetrieveMetadata = navigateToRetrieveMetadata,
                    navController = rightPaneNavController,
                    navigation = rightPaneNavigation
                )
            }

        }
    }
}

