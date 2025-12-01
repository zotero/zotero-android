package org.zotero.android.screens.collectionpicker

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.collectionpicker.table.CollectionsPickerTable
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun CollectionPickerScreen(
    onBack: () -> Unit,
    viewModel: CollectionPickerViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val viewState by viewModel.viewStates.observeAsState(CollectionPickerViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        val isTablet = layoutType.isTablet()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init(isTablet = isTablet)
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is CollectionPickerViewEffect.OnBack -> {
                    onBack()
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
                CollectionPickerTopBar(
                    title = viewState.title,
                    multipleSelectionAllowed = viewState.multipleSelectionAllowed,
                    scrollBehavior = scrollBehavior,
                    onBackClicked = onBack,
                    onAdd = viewModel::confirmSelection,
                )
            },
        ) {
            CollectionsPickerTable(
                viewState = viewState,
                viewModel = viewModel,
                layoutType = layoutType
            )
        }
    }
}