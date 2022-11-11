@file:OptIn(ExperimentalPagerApi::class)

package org.zotero.android.dashboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.pager.ExperimentalPagerApi
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.dashboard.DashboardViewModel
import org.zotero.android.dashboard.DashboardViewState
import org.zotero.android.uicomponents.systemui.SolidStatusBar

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun DashboardScreen(
    onBack: () -> Unit,
    onPickFile: () -> Unit,
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

    DashboardNavigation(onPickFile = onPickFile,)

}


