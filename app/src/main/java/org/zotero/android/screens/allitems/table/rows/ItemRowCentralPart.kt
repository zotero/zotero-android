package org.zotero.android.screens.allitems.table.rows

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun ItemRowCentralPart(
    model: ItemCellModel,
    itemAccessory: ItemCellModel.Accessory?,
    isEditing: Boolean,
    onAccessoryTapped: (key: String) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (model.title.isEmpty()) " " else model.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = CustomTheme.colors.allItemsRowTitleColor,
                    style = CustomTheme.typography.newHeadline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    var subtitleText = if (model.subtitle.isEmpty()) " " else model.subtitle
                    val shouldHideSubtitle =
                        model.subtitle.isEmpty() && (model.hasNote || !model.tagColors.isEmpty())
                    if (shouldHideSubtitle) {
                        subtitleText = ""
                    }
                    Text(
                        text = subtitleText,
                        style = CustomTheme.typography.newBody,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = CustomPalette.SystemGray,
                    )
                    if (model.hasNote) {
                        if(model.subtitle.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Image(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.CenterVertically),
                            painter = painterResource(id = Drawables.cell_note),
                            contentDescription = null,
                        )
                    }

                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            ItemRowRightPart(
                model = model,
                itemAccessory = itemAccessory,
                isEditing = isEditing,
                onAccessoryTapped = onAccessoryTapped,
            )
        }
    }
}