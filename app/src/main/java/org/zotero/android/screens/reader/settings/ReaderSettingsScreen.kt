package org.zotero.android.screens.reader.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.reader.settings.data.ReaderSettingsArgs
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun ReaderSettingsScreen(
    args: ReaderSettingsArgs?,
    onBack: () -> Unit,
    viewModel: ReaderSettingsViewModel = hiltViewModel(),
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
    val viewState by viewModel.viewStates.observeAsState(ReaderSettingsViewState())
    AppThemeM3(
        darkTheme = viewState.isDark,
    ) {
        CustomScaffoldM3(
            topBar = {
                ReaderSettingsTopBar(
                    onDone = sendParamsAndBack,
                )
            },
        ) {
            ReaderSettingsTable(viewState, viewModel)
        }
    }
}
