@file:OptIn(ExperimentalPagerApi::class)

package org.zotero.android.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.dashboard.DashboardViewModel
import org.zotero.android.dashboard.DashboardViewState
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.systemui.SolidStatusBar
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.HeadingTextButton
import org.zotero.android.uicomponents.topbar.NoIconTopBar

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun DashboardScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(DashboardViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
        }
    }
    SolidStatusBar()

    CustomScaffold(
        topBar = {
            TopBar(
                onCancelClicked = onBack,
            )
        },
        snackbarMessage = viewState.snackbarMessage,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = CustomTheme.colors.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {

            Column(
                modifier = Modifier
                    .widthIn(max = 430.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    text = stringResource(id = Strings.dashboard_temp_string),
                    color = CustomTheme.colors.zoteroBlueWithDarkMode,
                    style = CustomTheme.typography.default,
                    fontSize = layoutType.calculateTextSize(),
                )

            }
        }
    }

}

@Composable
private fun TopBar(
    onCancelClicked: () -> Unit,
) {
    NoIconTopBar(
        title = "",
    ) {
        HeadingTextButton(
            onClick = onCancelClicked,
            text = stringResource(id = Strings.cancel),
            isEnabled = true,
            isLoading = false,
            modifier = Modifier
                .padding(end = 8.dp)
        )
    }
}
