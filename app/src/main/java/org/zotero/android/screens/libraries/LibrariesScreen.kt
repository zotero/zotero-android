package org.zotero.android.screens.libraries

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.libraries.table.LibrariesTable
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.error.FullScreenError
import org.zotero.android.uicomponents.loading.BaseLceBox
import org.zotero.android.uicomponents.loading.CircularLoading
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun LibrariesScreen(
    viewModel: LibrariesViewModel = hiltViewModel(),
    navigateToCollectionsScreen: (String) -> Unit,
    onSettingsTapped: () -> Unit,
    onExitApp:() -> Unit,
) {
    AppThemeM3 {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val viewState by viewModel.viewStates.observeAsState(LibrariesViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()

        BackHandler(
            enabled = viewState.backHandlerEnabled,
            onBack = {
                onExitApp()
            })

        LaunchedEffect(key1 = viewModel) {
            viewModel.init(isTablet = layoutType.isTablet())
        }

        LaunchedEffect(key1 = viewEffect) {
            val consumedEffect = viewEffect?.consume()
            when (consumedEffect) {
                null -> Unit
                is LibrariesViewEffect.NavigateToCollectionsScreen -> navigateToCollectionsScreen(
                    consumedEffect.screenArgs
                )
            }
        }

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        CustomScaffoldM3(
            scrollBehavior = scrollBehavior,
            topBar = {
                LibrariesTopBar(
                    scrollBehavior = scrollBehavior,
                    onSettingsTapped = onSettingsTapped,
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
                LibrariesTable(
                    viewState = viewState,
                    viewModel = viewModel,
                )
            }
        }
    }
}