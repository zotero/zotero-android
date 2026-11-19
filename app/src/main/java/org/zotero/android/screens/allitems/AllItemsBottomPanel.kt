package org.zotero.android.screens.allitems

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable

@Composable
internal fun AllItemsBottomPanelNew(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
) {
    Column {
        AllItemsDownloadProgress(batchState = viewState.downloadBatchState)
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
}
