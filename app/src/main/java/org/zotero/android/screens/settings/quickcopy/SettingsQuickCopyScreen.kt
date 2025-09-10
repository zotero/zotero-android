package org.zotero.android.screens.settings.quickcopy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun SettingsQuickCopyScreen(
    onBack: () -> Unit,
    navigateToStylePicker: () -> Unit,
    navigateToCslLocalePicker: () -> Unit,
    viewModel: SettingsQuickCopyViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(SettingsQuickCopyViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is SettingsQuickCopyViewEffect.OnBack -> {
                    onBack()
                }

                is SettingsQuickCopyViewEffect.NavigateToStylePicker -> {
                    navigateToStylePicker()
                }

                is SettingsQuickCopyViewEffect.NavigateToCslLocalePicker -> {
                    navigateToCslLocalePicker()
                }
            }
        }
        CustomScaffoldM3(
            topBar = {
                SettingsQuickCopyTopBar(
                    onBack = onBack,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                SettingsQuickCopySections(
                    selectedStyle = viewState.selectedStyle,
                    selectedLanguage = viewState.selectedLanguage,
                    languagePickerEnabled = viewState.languagePickerEnabled,
                    copyAsHtml = viewState.copyAsHtml,
                    onDefaultFormatTapped = viewModel::onDefaultFormatTapped,
                    onLanguageTapped = viewModel::onLanguageTapped,
                    onQuickCopySwitchTapped = viewModel::onQuickCopySwitchTapped,
                )
            }
        }

    }
}
