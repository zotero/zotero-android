package org.zotero.android.screens.itemdetails.rows.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.itemdetails.AddItemRow
import org.zotero.android.screens.itemdetails.DatesRows
import org.zotero.android.screens.itemdetails.ItemDetailsViewModel
import org.zotero.android.screens.itemdetails.ItemDetailsViewState
import org.zotero.android.screens.itemdetails.rows.ItemDetailsItemType
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.reorder.ReorderableState

internal fun LazyListScope.itemDetailsEditDataRows(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
    reorderState: ReorderableState
) {
    item {
        ItemDetailsItemType(
            viewState = viewState,
            onItemTypeClicked = viewModel::onItemTypeClicked
        )
    }
    if (!viewState.data.isAttachment) {
        itemDetailsEditListOfCreatorRows(
            viewState = viewState,
            onDeleteCreator = viewModel::onDeleteCreator,
            onCreatorClicked = viewModel::onCreatorClicked,
            reorderState = reorderState,
        )
    }
    item {
        if (!viewState.data.isAttachment) {
            AddItemRow(
                titleRes = Strings.item_detail_add_creator,
                onClick = viewModel::onAddCreator
            )
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            ItemDetailsEditListOfFieldRows(
                viewState = viewState,
                onValueChange = viewModel::onFieldValueTextChange,
                onFocusChanges = viewModel::onFieldFocusFieldChange
            )
            DatesRows(
                dateAdded = viewState.data.dateAdded,
                dateModified = viewState.data.dateModified,
                isEditMode = true,
            )
        }
        if (!viewState.data.isAttachment) {
            NewSettingsDivider()
            ItemDetailsEditAbstractFieldRow(
                detailValue = viewState.abstractText,
                onValueChange = viewModel::onAbstractTextChange
            )
        }
    }
}
