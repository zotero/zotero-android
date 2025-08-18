package org.zotero.android.screens.settings.stylepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun SettingsStylePickerScreen(
    onBack: () -> Unit,
    viewModel: SettingsStylePickerViewModel = hiltViewModel(),
) {
    CustomThemeWithStatusAndNavBars {
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
        CustomScaffold(
            topBarColor = CustomTheme.colors.surface,
            bottomBarColor = CustomTheme.colors.zoteroItemDetailSectionBackground,
            topBar = {
                SettingsCiteTopBar(
                    onBack = onBack,
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = CustomTheme.colors.zoteroItemDetailSectionBackground)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                    SettingsCiteCitationStylesSection(viewState, viewModel)
                }
            }

        }
    }
}
