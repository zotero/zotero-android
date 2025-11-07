package org.zotero.android.screens.allitems.table.rows

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.attachmentprogress.FileAttachmentView
import org.zotero.android.uicomponents.attachmentprogress.Style

@Composable
internal fun RowScope.ItemRowSetAccessory(
    accessory: ItemCellModel.Accessory?,
) {
    when (accessory) {
        is ItemCellModel.Accessory.attachment -> {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                FileAttachmentView(
                    modifier = Modifier
                        .size(20.dp),
                    state = accessory.state,
                    style = Style.list,
                    mainIconSize = 16.dp,
                    badgeIconSize = 10.dp,
                )
            }
        }

        is ItemCellModel.Accessory.doi, is ItemCellModel.Accessory.url -> {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(Drawables.list_link),
                    contentDescription = null,
                    tint = null,
                )
            }

        }
        else -> {
            //no-op
        }
    }
}
