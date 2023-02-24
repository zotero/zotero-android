package org.zotero.android.screens.dashboard

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import org.zotero.android.architecture.EventBusConstants.FileWasSelected.CallPoint
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.systemui.SolidStatusBar
import java.io.File

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun DashboardScreen(
    onBack: () -> Unit,
    onPickFile: (callPoint: CallPoint) -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    viewModel: DashboardViewModel,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(DashboardViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    val context = LocalContext.current
    LaunchedEffect(key1 = viewModel) {
        viewModel.init(context = context)
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
        }
    }
    SolidStatusBar()

    DashboardNavigation(
        onPickFile = onPickFile,
        onOpenWebpage = onOpenWebpage,
        onOpenFile = onOpenFile
    )

}


