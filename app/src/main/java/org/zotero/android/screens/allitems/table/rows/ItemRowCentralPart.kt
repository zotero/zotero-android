package org.zotero.android.screens.allitems.table.rows

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.allitems.data.ItemCellModel

@Composable
internal fun RowScope.ItemRowCentralPart(
    model: ItemCellModel,
    itemAccessory: ItemCellModel.Accessory?,
    isEditing: Boolean,
    onAccessoryTapped: (key: String) -> Unit,
    isItemSelected: (key: String) -> Boolean,
    onItemTapped: (item: ItemCellModel) -> Unit,
) {
    ItemRowTitleAndSubtitlePart(model)
    Spacer(modifier = Modifier.width(8.dp))
    ItemRowRightPart(
        model = model,
        itemAccessory = itemAccessory,
        isEditing = isEditing,
        onAccessoryTapped = onAccessoryTapped,
        isItemSelected = isItemSelected,
        onItemTapped = onItemTapped,
    )
}