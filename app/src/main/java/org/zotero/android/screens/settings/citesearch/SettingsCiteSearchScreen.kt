package org.zotero.android.screens.settings.citesearch

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.error.FullScreenError
import org.zotero.android.uicomponents.loading.BaseLceBox
import org.zotero.android.uicomponents.loading.CircularLoading
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun SettingsCiteSearchScreen(
    onBack: () -> Unit,
    viewModel: SettingsCiteSearchViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(SettingsCiteSearchViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is SettingsCiteSearchViewEffect.OnBack -> {
                    onBack()
                }
            }
        }
        CustomScaffoldM3(
            topBar = {
                SettingsCiteSearchTopBar(
                    onBack = onBack,
                    viewModel = viewModel,
                    viewState = viewState,
                )
            },
        ) {
            BaseLceBox(
                modifier = Modifier.fillMaxSize(),
                lce = viewState.lce,
                error = { _ ->
                    FullScreenError(
                        modifier = Modifier.align(Alignment.Center),
                        errorTitle = stringResource(id = Strings.error_list_load_check_crash_logs),
                    )
                },
                loading = {
                    CircularLoading()
                },
            ) {
                SettingsCiteSearchStylesTable(viewState, viewModel)
            }
        }
    }
}
