package org.zotero.android.screens.sortpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun SortPickerScreen(
    onBack: () -> Unit,
    navigateToSinglePickerScreen: () -> Unit,
    viewModel: SortPickerViewModel = hiltViewModel(),
) {
    CustomThemeWithStatusAndNavBars {
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

                is SortPickerViewEffect.NavigateToSinglePickerScreen -> {
                    navigateToSinglePickerScreen()
                }
            }
        }
        CustomScaffold(
            topBarColor = CustomTheme.colors.topBarBackgroundColor,
            topBar = {
                SortPickerTopBar(
                    onDone = viewModel::onDone,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .background(color = CustomTheme.colors.surface)
            ) {
                SortPickerDisplayFields(
                    sortByTitle = viewState.sortByTitle,
                    isAscending = viewState.isAscending,
                    onSortFieldClicked = viewModel::onSortFieldClicked,
                    onSortDirectionChanged = viewModel::onSortDirectionChanged,
                )
            }
        }
    }
}