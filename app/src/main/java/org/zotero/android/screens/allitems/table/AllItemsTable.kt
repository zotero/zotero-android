package org.zotero.android.screens.allitems.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.screens.allitems.table.rows.ItemRow

@Composable
internal fun AllItemsTable(
    lazyListState: LazyListState,
    layoutType: CustomLayoutSize.LayoutType,
    itemCellModels: SnapshotStateList<ItemCellModel>,
    isItemSelected: (key: String) -> Boolean,
    getItemAccessory: (itemKey: String) -> ItemCellModel.Accessory?,
    isEditing: Boolean,
    onItemTapped: (item: ItemCellModel) -> Unit,
    onItemLongTapped: (key: String) -> Unit,
    onAccessoryTapped: (key: String) -> Unit,
    pullRefreshState: PullRefreshState,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState),
        state = lazyListState,
    ) {
        itemsIndexed(
            items = itemCellModels, key = { _, item -> item.hashCode() }
        ) { index, item ->
            Box(modifier = Modifier.animateItemPlacement()) {
                ItemRow(
                    cellModel = item,
                    itemAccessory = getItemAccessory(item.key),
                    layoutType = layoutType,
                    showBottomDivider = index != itemCellModels.size - 1,
                    isEditing = isEditing,
                    onItemTapped = onItemTapped,
                    onItemLongTapped = onItemLongTapped,
                    onAccessoryTapped = onAccessoryTapped,
                    isItemSelected = isItemSelected,
                )
            }
        }
    }
}

