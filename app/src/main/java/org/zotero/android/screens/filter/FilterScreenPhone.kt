package org.zotero.android.screens.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun FilterScreenPhone(
    viewModel: FilterViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val viewState by viewModel.viewStates.observeAsState(FilterViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        val args = viewModel.phoneScreenArgs
        viewModel.init(args)
    }

    LaunchedEffect(key1 = viewEffect) {
        when (viewEffect?.consume()) {
            is FilterViewEffect.OnBack -> {
                onBack()
            }

            else -> {}
        }
    }
    AppThemeM3 {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        CustomScaffoldM3(
            scrollBehavior = scrollBehavior,
            topBar = {
                FilterTopBar(
                    onDone = viewModel::onDone,
                    viewState = viewState,
                    viewModel = viewModel,
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .fillMaxWidth()
                        .windowInsetsPadding(BottomAppBarDefaults.windowInsets)
                )
            }
        ) {
            FilterScreen(viewModel = viewModel, viewState = viewState)
        }
    }

}
