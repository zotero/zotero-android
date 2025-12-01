package org.zotero.android.screens.settings.stylepicker

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun SettingsStylePickerScreen(
    onBack: () -> Unit,
    viewModel: SettingsStylePickerViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(SettingsStylePickerViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is SettingsStylePickerViewEffect.OnBack -> {
                    onBack()
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
