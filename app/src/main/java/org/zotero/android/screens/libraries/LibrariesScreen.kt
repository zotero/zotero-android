package org.zotero.android.screens.libraries

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.error.FullScreenError
import org.zotero.android.uicomponents.loading.BaseLceBox
import org.zotero.android.uicomponents.loading.CircularLoading
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun LibrariesScreen(
    navigateToCollectionsScreen: (String) -> Unit,
    onSettingsTapped: () -> Unit,
    viewModel: LibrariesViewModel = hiltViewModel(),
) {
    val backgroundColor = CustomTheme.colors.pdfAnnotationsFormBackground
    CustomThemeWithStatusAndNavBars(
        statusBarBackgroundColor = CustomTheme.colors.topBarBackgroundColor,
        navBarBackgroundColor = backgroundColor,
    ) {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val viewState by viewModel.viewStates.observeAsState(LibrariesViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init(isTablet = layoutType.isTablet())
        }

        LaunchedEffect(key1 = viewEffect) {
            val consumedEffect = viewEffect?.consume()
            when (consumedEffect) {
                null -> Unit
                is LibrariesViewEffect.NavigateToCollectionsScreen -> navigateToCollectionsScreen(consumedEffect.screenArgs)
            }
        }

        CustomScaffold(
            backgroundColor = backgroundColor,
            topBar = {
                LibrariesTopBar(
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
                Column {
                    LibrariesTable(
                        viewState = viewState,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}