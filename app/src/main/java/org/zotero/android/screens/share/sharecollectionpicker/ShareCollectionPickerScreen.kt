package org.zotero.android.screens.share.sharecollectionpicker

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun ShareCollectionPickerScreen(
    onBack: () -> Unit,
    viewModel: ShareCollectionPickerViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(ShareCollectionPickerViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is ShareCollectionPickerViewEffect.OnBack -> {
                    onBack()
                }
                is ShareCollectionPickerViewEffect.ScreenRefresh -> {
                }

                else -> {
                    //no-op
                }
            }
        }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        CustomScaffoldM3(
            scrollBehavior = scrollBehavior,
            topBar = {
                ShareCollectionPickerTopBar(
                    scrollBehavior = scrollBehavior,
                    onBack = onBack,
                )
            },
        ) {
            ShareCollectionsPickerTable(
                viewState = viewState,
                viewModel = viewModel,
            )
        }
    }
}