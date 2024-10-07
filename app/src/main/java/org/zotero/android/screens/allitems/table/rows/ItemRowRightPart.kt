package org.zotero.android.screens.allitems.table.rows

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.attachmentprogress.FileAttachmentView
import org.zotero.android.uicomponents.attachmentprogress.Style
import org.zotero.android.uicomponents.icon.IconWithPadding

@Composable
internal fun RowScope.ItemRowRightPart(
    model: ItemCellModel,
    itemAccessory: ItemCellModel.Accessory?,
    isEditing: Boolean,
    onAccessoryTapped: (key: String) -> Unit,
) {
    SetAccessory(
        accessory = itemAccessory,
    )
    AnimatedContent(
        modifier = Modifier.align(Alignment.CenterVertically),
        targetState = isEditing,
        label = ""
    ) { isEditing ->
        if (!isEditing) {
            Row {
                IconWithPadding(
                    onClick = {
                        onAccessoryTapped(model.key)
                    },
                    drawableRes = Drawables.info_24px
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}


@Composable
private fun RowScope.SetAccessory(
    accessory: ItemCellModel.Accessory?,
) {
    if (accessory == null) {
        return
    }
    when (accessory) {
        is ItemCellModel.Accessory.attachment -> {
            IconWithPadding(content = {
                FileAttachmentView(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterVertically),
                    state = accessory.state,
                    style = Style.list,
                    mainIconSize = 16.dp,
                    badgeIconSize = 10.dp,
                )
            })
        }

        is ItemCellModel.Accessory.doi, is ItemCellModel.Accessory.url -> {
            IconWithPadding(drawableRes = Drawables.list_link, iconSize = 16.dp, tintColor = null)
        }
        else -> {
            //no-op
        }
    }
}
