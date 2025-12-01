package org.zotero.android.screens.settings.cite

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun SettingsCiteScreen(
    onBack: () -> Unit,
    navigateToCiteSearch: (String) -> Unit,
    viewModel: SettingsCiteViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(SettingsCiteViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                null -> Unit
                is SettingsCiteViewEffect.OnBack -> {
                    onBack()
                }

                is SettingsCiteViewEffect.NavigateToCiteSearch -> {
                    navigateToCiteSearch(consumedEffect.args)
                }
            }
        }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        CustomScaffoldM3(
            scrollBehavior = scrollBehavior,
            topBar = {
                SettingsCiteTopBar(
                    scrollBehavior = scrollBehavior,
                    onBack = onBack,
                )
            },
        ) {
            SettingsCiteCitationStylesSection(viewState, viewModel)
        }
    }
}
