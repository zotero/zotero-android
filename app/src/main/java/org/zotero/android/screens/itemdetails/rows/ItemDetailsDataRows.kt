package org.zotero.android.screens.itemdetails.rows

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.itemdetails.DatesRows
import org.zotero.android.screens.itemdetails.ItemDetailsViewModel
import org.zotero.android.screens.itemdetails.ItemDetailsViewState
import org.zotero.android.uicomponents.Strings

@Composable
internal fun ItemDetailsDataRows(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        ItemDetailsFieldRow(
            detailTitle = stringResource(id = Strings.item_type),
            detailValue = viewState.data.localizedType,
        )
        ItemDetailsListOfCreatorRows(
            viewState = viewState,
            onCreatorLongClick = viewModel::onCreatorLongClick
        )
        ItemDetailsListOfFieldRows(
            viewState = viewState,
            viewModel = viewModel
        )
        DatesRows(
            dateAdded = viewState.data.dateAdded,
            dateModified = viewState.data.dateModified,
            isEditMode = false
        )
    }
}
