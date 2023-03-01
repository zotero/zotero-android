package org.zotero.android.screens.dashboard

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.zotero.android.architecture.EventBusConstants.FileWasSelected.CallPoint
import org.zotero.android.uicomponents.systemui.SolidStatusBar
import java.io.File

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun DashboardScreen(
    onBack: () -> Unit,
    onPickFile: (callPoint: CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onShowPdf: (file: File) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    viewModel: DashboardViewModel,
) {
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    SolidStatusBar()

    DashboardNavigation(
        viewModel = viewModel,
        onPickFile = onPickFile,
        onOpenWebpage = onOpenWebpage,
        onOpenFile = onOpenFile,
        onShowPdf = onShowPdf
    )

}


