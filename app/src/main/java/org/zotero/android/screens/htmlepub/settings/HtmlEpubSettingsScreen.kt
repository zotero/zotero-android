package org.zotero.android.screens.htmlepub.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.htmlepub.settings.data.HtmlEpubSettingsArgs
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun HtmlEpubSettingsScreen(
    args: HtmlEpubSettingsArgs?,
    onBack: () -> Unit,
    viewModel: HtmlEpubSettingsViewModel = hiltViewModel(),
) {
    val sendParamsAndBack: () -> Unit = {
        viewModel.sendSettingsParams()
        onBack()
    }

    BackHandler(onBack = {
        sendParamsAndBack()
    })
    LaunchedEffect(Unit) {
        viewModel.init(args = args)
    }

    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(HtmlEpubSettingsViewState())
    AppThemeM3(
        darkTheme = viewState.isDark,
    ) {
        CustomScaffoldM3(
            topBar = {
                HtmlEpubSettingsTopBar(
                    onDone = sendParamsAndBack,
                )
            },
        ) {
            HtmlEpubSettingsTable(viewState, viewModel)
        }
    }
}
