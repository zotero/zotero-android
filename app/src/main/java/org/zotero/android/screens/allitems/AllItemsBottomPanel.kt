package org.zotero.android.screens.allitems

import androidx.compose.runtime.Composable

@Composable
internal fun AllItemsBottomPanelNew(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
) {
    if (viewState.isEditing) {
        AllItemsEditingBottomPanel(
            viewModel = viewModel,
            viewState = viewState,
        )
    } else {
        AllItemsRegularBottomPanel(
            viewModel = viewModel,
            viewState = viewState,
        )
    }
}
