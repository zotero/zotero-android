package org.zotero.android.screens.settings.csllocalepicker

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun SettingsCslLocalePickerScreen(
    onBack: () -> Unit,
    viewModel: SettingsCslLocalePickerViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(SettingsCslLocalePickerViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is SettingsCslLocalePickerViewEffect.OnBack -> {
                    onBack()
                }
            }
        }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        CustomScaffoldM3(
            scrollBehavior = scrollBehavior,
            topBar = {
                SettingsCslLocalePickerTopBar(
                    scrollBehavior = scrollBehavior,
                    onBack = onBack,
                )
            },
        ) {
            SettingsCslLocalePickerSections(viewState, viewModel)
        }
    }
}
