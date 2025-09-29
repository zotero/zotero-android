package org.zotero.android.screens.allitems.table.rows

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.uicomponents.Drawables

@Composable
internal fun RowScope.ItemRowRightPart(
    model: ItemCellModel,
    itemAccessory: ItemCellModel.Accessory?,
    isEditing: Boolean,
    onAccessoryTapped: (key: String) -> Unit,
    isItemSelected: (key: String) -> Boolean
) {
    ItemRowSetAccessory(
        accessory = itemAccessory,
    )
    Spacer(modifier = Modifier.width(8.dp))
    AnimatedContent(
        modifier = Modifier.align(Alignment.CenterVertically),
        targetState = isEditing,
        label = ""
    ) { isEditing ->
        if (isEditing) {
            IconButton(onClick = { } ) {
                if (isItemSelected(model.key)) {
                    Icon(
                        painter = painterResource(Drawables.check_circle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        painter = painterResource(Drawables.radio_button_unchecked),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            IconButton(onClick = { onAccessoryTapped(model.key) }) {
                Icon(
                    painter = painterResource(Drawables.info_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}


