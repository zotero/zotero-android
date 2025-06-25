package org.zotero.android.screens.settings.citesearch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.error.FullScreenError
import org.zotero.android.uicomponents.loading.BaseLceBox
import org.zotero.android.uicomponents.loading.CircularLoading
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun SettingsCiteSearchScreen(
    onBack: () -> Unit,
    viewModel: SettingsCiteSearchViewModel = hiltViewModel(),
) {
    CustomThemeWithStatusAndNavBars(
        navBarBackgroundColor = CustomTheme.colors.zoteroItemDetailSectionBackground,
    ) {
        val viewState by viewModel.viewStates.observeAsState(SettingsCiteSearchViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                null -> Unit
                is SettingsCiteSearchViewEffect.OnBack -> {
                    onBack()
                }
            }
        }
        CustomScaffold(
            backgroundColor = CustomTheme.colors.popupBackgroundContent,
            topBar = {
                SettingsCiteSearchTopBar(
                    onBack = onBack,
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
                Column(
                    modifier = Modifier
                ) {
                    Column(modifier = Modifier.background(CustomTheme.colors.topBarBackgroundColor)) {
                        SettingsCiteSearchSearchBar(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            viewState = viewState,
                            viewModel = viewModel
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        NewDivider()
                    }
                    SettingsCiteSearchStylesTable(viewState, viewModel)
                }
            }
        }
    }
}
