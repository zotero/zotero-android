package org.zotero.android.pdf.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.pdf.settings.data.PdfSettingsArgs
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun PdfSettingsScreen(
    args: PdfSettingsArgs?,
    onBack: () -> Unit,
    viewModel: PdfSettingsViewModel = hiltViewModel(),
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
    val viewState by viewModel.viewStates.observeAsState(PdfSettingsViewState())
    AppThemeM3(
        darkTheme = viewState.isDark,
    ) {
        CustomScaffoldM3(
            topBar = {
                PdfSettingsTopBar(
                    onDone = sendParamsAndBack,
                )
            },
        ) {
            PdfSettingsTable(viewState, viewModel)
        }
    }
}



