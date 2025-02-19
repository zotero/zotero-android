package org.zotero.android.screens.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.filter.data.FilterArgs

@Composable
internal fun FilterScreenTablet(
    filterArgs: FilterArgs,
    viewModel: FilterViewModel = hiltViewModel(),
) {

    val viewState by viewModel.viewStates.observeAsState(FilterViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init(filterArgs)
    }

    FilterScreen(viewModel = viewModel, viewState = viewState)
}