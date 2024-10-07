package org.zotero.android.screens.allitems.table.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun ItemRow(
    cellModel: ItemCellModel,
    itemAccessory: ItemCellModel.Accessory?,
    isItemSelected: (key: String) -> Boolean,
    layoutType: CustomLayoutSize.LayoutType,
    showBottomDivider: Boolean = false,
    isEditing: Boolean,
    onItemTapped: (item: ItemCellModel) -> Unit,
    onItemLongTapped: (key: String) -> Unit,
    onAccessoryTapped: (key: String) -> Unit,
) {
    var rowModifier: Modifier = Modifier.height(64.dp)
    if (isItemSelected(cellModel.key)) {
        rowModifier = rowModifier.background(color = CustomTheme.colors.popupSelectedRow)
    }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = rowModifier
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = if (isEditing) null else rememberRipple(),
                    onClick = { onItemTapped(cellModel) },
                    onLongClick = { onItemLongTapped(cellModel.key) }
                )
        ) {
            ItemRowLeftPart(
                layoutType = layoutType,
                model = cellModel,
                isEditing = isEditing,
                isItemSelected = isItemSelected,
            )
            Spacer(modifier = Modifier.width(16.dp))
            ItemRowCentralPart(
                model = cellModel,
                isEditing = isEditing,
                itemAccessory = itemAccessory,
                onAccessoryTapped = onAccessoryTapped,
            )
        }
        if (showBottomDivider) {
            CustomDivider(modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 60.dp))
        }
    }
}