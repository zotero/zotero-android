package org.zotero.android.screens.allitems

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.attachmentprogress.FileAttachmentView
import org.zotero.android.uicomponents.attachmentprogress.Style
import org.zotero.android.uicomponents.checkbox.CircleCheckBox
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun AllItemsTable(
    layoutType: CustomLayoutSize.LayoutType,
    itemCellModels: SnapshotStateList<ItemCellModel>,
    isItemSelected: (key: String) -> Boolean,
    getItemAccessory: (itemKey: String) -> ItemCellModel.Accessory?,
    isEditing: Boolean,
    onItemTapped: (item: ItemCellModel) -> Unit,
    onItemLongTapped: (key: String) -> Unit,
    onAccessoryTapped: (key: String) -> Unit,
) {
    LazyColumn(
        state = rememberLazyListState(),
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

@Composable
private fun ItemRow(
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

@Composable
private fun ItemRowLeftPart(
    layoutType: CustomLayoutSize.LayoutType,
    model: ItemCellModel,
    isItemSelected: (key: String) -> Boolean,
    isEditing: Boolean,
) {
    AnimatedContent(targetState = isEditing, label = "") { isEditing ->
        if (isEditing) {
            Row {
                Spacer(modifier = Modifier.width(16.dp))
                CircleCheckBox(
                    isChecked = isItemSelected(model.key),
                    layoutType = layoutType
                )
            }
        }
    }
    Spacer(modifier = Modifier.width(16.dp))
    Image(
        modifier = Modifier.size(28.dp),
        painter = painterResource(id = model.typeIconName),
        contentDescription = null,
    )
}

@Composable
private fun ItemRowCentralPart(
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

@Composable
private fun RowScope.ItemRowRightPart(
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
