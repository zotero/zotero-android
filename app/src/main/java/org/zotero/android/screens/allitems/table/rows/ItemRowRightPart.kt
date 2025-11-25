package org.zotero.android.screens.allitems.table.rows

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
internal fun RowScope.ItemRowRightPart(
    model: ItemCellModel,
    itemAccessory: ItemCellModel.Accessory?,
    isEditing: Boolean,
    onAccessoryTapped: (key: String) -> Unit,
    isItemSelected: (key: String) -> Boolean,
    onItemTapped: (item: ItemCellModel) -> Unit,
) {
    ItemRowSetAccessory(
        accessory = itemAccessory,
    )
//    Spacer(modifier = Modifier.width(8.dp))
    AnimatedContent(
        modifier = Modifier.align(Alignment.CenterVertically),
        targetState = isEditing,
        label = ""
    ) { isEditing ->
        if (isEditing) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = {
                        onItemTapped(model)
                    }),
                contentAlignment = Alignment.Center
            ) {
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
            TooltipBox(
                positionProvider = rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above,
                    10.dp
                ),
                tooltip = {
                    PlainTooltip() {
                        Text(
                            stringResource(
                                Strings.all_items_details
                            )
                        )
                    }
                },
                state = rememberTooltipState()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = {
                            onAccessoryTapped(model.key)
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Drawables.info_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}


