package org.zotero.android.screens.allitems.table.rows

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.androidx.content.getDrawableByItemType
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.uicomponents.foundation.debounceCombinedClickable

@Composable
internal fun ItemRow(
    cellModel: ItemCellModel,
    itemAccessory: ItemCellModel.Accessory?,
    isItemSelected: (key: String) -> Boolean,
    isEditing: Boolean,
    onItemTapped: (item: ItemCellModel) -> Unit,
    onItemLongTapped: (key: String) -> Unit,
    onAccessoryTapped: (key: String) -> Unit,
) {
    var rowModifier: Modifier = Modifier.height(64.dp)
    val isRowSelected = isItemSelected(cellModel.key)
    if (isRowSelected) {
        val roundCornerShape = RoundedCornerShape(8.dp)
        rowModifier = rowModifier
            .padding(horizontal = 8.dp,  vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = roundCornerShape)
            .clip(roundCornerShape)

    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier
            .debounceCombinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = if (isEditing) null else ripple(),
                onClick = { onItemTapped(cellModel) },
                onLongClick = { onItemLongTapped(cellModel.key) }
            )
    ) {
        if (isRowSelected) {
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }
        Image(
            modifier = Modifier.size(28.dp),
            painter = painterResource(id = LocalContext.current.getDrawableByItemType(cellModel.typeIconName)),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(12.dp))
        ItemRowCentralPart(
            model = cellModel,
            isEditing = isEditing,
            itemAccessory = itemAccessory,
            onAccessoryTapped = onAccessoryTapped,
            isItemSelected = isItemSelected,
            onItemTapped = onItemTapped
        )
        if (!isRowSelected) {
            Spacer(modifier = Modifier.width(8.dp))
        }

    }
}