package org.zotero.android.screens.sortpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun SortPickerScreen(
    onBack: () -> Unit,
    viewModel: SortPickerViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(SortPickerViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is SortPickerViewEffect.OnBack -> {
                    onBack()
                }
            }
        }
        CustomScaffoldM3(
            topBar = {
                SortPickerTopBar(
                    onDone = viewModel::onDone,
                )
            },
        ) {
            LazyColumn(
            ) {
                sortPickerSortByTable(viewState = viewState, viewModel = viewModel)
                item {
                    NewSettingsDivider()
                }
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SortPickerSortDirectionSelector(
                            viewState = viewState,
                            viewModel = viewModel
                        )
                    }
                }
            }

        }
    }
}