package org.zotero.android.screens.collections

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
import org.zotero.android.screens.collections.CollectionsViewEffect.NavigateBack
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.error.FullScreenError
import org.zotero.android.uicomponents.loading.BaseLceBox
import org.zotero.android.uicomponents.loading.CircularLoading
import org.zotero.android.uicomponents.misc.CustomDivider

@Composable
internal fun CollectionsScreen(
    onBack: () -> Unit,
    navigateToAllItems: () -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(CollectionsViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init(isTablet = layoutType.isTablet())
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
            NavigateBack -> onBack()
            CollectionsViewEffect.NavigateToAllItemsScreen -> navigateToAllItems()
        }
    }

    CustomScaffold(
        topBar = {
            CollectionsTopBar(
                viewState = viewState,
                viewModel = viewModel,
            )
        },
    ) {
        BaseLceBox(
            modifier = Modifier.fillMaxSize(),
            lce = viewState.lce,
            error = { lceError ->
                FullScreenError(
                    modifier = Modifier.align(Alignment.Center),
                    errorTitle = stringResource(id = Strings.all_items_load_error),
                )
            },
            loading = {
                CircularLoading()
            },
        ) {
            Column {
                CustomDivider()
                CollectionsTable(
                    viewState = viewState,
                    viewModel = viewModel,
                    layoutType = layoutType
                )
            }
        }
    }
}