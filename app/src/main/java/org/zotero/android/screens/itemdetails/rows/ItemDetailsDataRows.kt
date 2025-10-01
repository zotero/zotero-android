package org.zotero.android.screens.itemdetails.rows

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.itemdetails.DatesRows
import org.zotero.android.screens.itemdetails.ItemDetailsViewModel
import org.zotero.android.screens.itemdetails.ItemDetailsViewState

@Composable
internal fun ItemDetailsDataRows(
    viewState: ItemDetailsViewState,
    layoutType: CustomLayoutSize.LayoutType,
    viewModel: ItemDetailsViewModel
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        ItemDetailsItemType(viewState, layoutType)
        ItemDetailsListOfCreatorRows(
            viewState = viewState,
            layoutType = layoutType,
            onCreatorLongClick = viewModel::onCreatorLongClick
        )
        ItemDetailsListOfFieldRows(
            viewState = viewState,
            layoutType = layoutType,
            viewModel = viewModel
        )
        DatesRows(
            dateAdded = viewState.data.dateAdded,
            dateModified = viewState.data.dateModified,
            layoutType = layoutType,
        )
    }
}
