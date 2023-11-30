package org.zotero.android.screens.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun FilterScreenPhone(viewModel: FilterViewModel = hiltViewModel(), onBack: () -> Unit) {
    val viewState by viewModel.viewStates.observeAsState(FilterViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (viewEffect?.consume()) {
            is FilterViewEffect.OnBack -> {
                onBack()
            }

            else -> {}
        }
    }
    val backgroundColor = CustomTheme.colors.popupBackgroundContent

    CustomThemeWithStatusAndNavBars(
        statusBarBackgroundColor = CustomTheme.colors.topBarBackgroundColor,
        navBarBackgroundColor = backgroundColor
    ) {
        CustomScaffold(
            backgroundColor = backgroundColor,
            topBar = {
                FilterTopBar(
                    onDone = viewModel::onDone,
                )
            },
        ) {
            FilterScreen(viewModel = viewModel, viewState = viewState)
        }
    }

}
