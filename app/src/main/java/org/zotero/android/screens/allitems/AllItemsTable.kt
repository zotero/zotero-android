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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.RItem
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.attachmentprogress.FileAttachmentView
import org.zotero.android.uicomponents.attachmentprogress.Style
import org.zotero.android.uicomponents.checkbox.CircleCheckBox
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.loading.IconLoadingPlaceholder
import org.zotero.android.uicomponents.loading.TextLoadingPlaceholder
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun AllItemsTable(
    viewState: AllItemsViewState,
    layoutType: CustomLayoutSize.LayoutType,
    viewModel: AllItemsViewModel
) {
    LazyColumn(
        state = rememberLazyListState(),
    ) {
        itemsIndexed(
            items = viewState.snapshot!!
        ) { index, item ->
            Box(modifier = Modifier.animateItemPlacement()) {
                val model = viewState.itemKeyToItemCellModelMap[item.key]
                if (model == null) {
                    ItemPlaceHolderRow(layoutType = layoutType)
                } else {
                    ItemRow(
                        rItem = item,
                        model = model,
                        layoutType = layoutType,
                        viewState = viewState,
                        viewModel = viewModel,
                        showBottomDivider = index != viewState.snapshot.size - 1
                    )
                }

            }
        }
    }
}

@Composable
private fun ItemRow(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
    rItem: RItem,
    model: ItemCellModel,
    layoutType: CustomLayoutSize.LayoutType,
    showBottomDivider: Boolean = false
) {
    var rowModifier: Modifier = Modifier
    if (viewState.selectedItems.contains(rItem.key)) {
        rowModifier = rowModifier.background(color = CustomTheme.colors.popupSelectedRow)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = if (viewState.isEditing) null else rememberRipple(),
                onClick = { viewModel.onItemTapped(rItem) },
                onLongClick = { viewModel.onItemLongTapped(rItem) }
            )
    ) {
        ItemRowLeftPart(
            viewState = viewState,
            rItem = rItem,
            layoutType = layoutType,
            model = model
        )
        ItemRowCentralPart(
            model = model,
            layoutType = layoutType,
            viewState = viewState,
            viewModel = viewModel,
            rItem = rItem,
            showBottomDivider = showBottomDivider
        )
    }
}

@Composable
private fun ItemRowLeftPart(
    viewState: AllItemsViewState,
    rItem: RItem,
    layoutType: CustomLayoutSize.LayoutType,
    model: ItemCellModel
) {
    AnimatedContent(targetState = viewState.isEditing) { isEditing ->
        if (isEditing) {
            Row {
                Spacer(modifier = Modifier.width(16.dp))
                CircleCheckBox(
                    isChecked = viewState.selectedItems.contains(rItem.key),
                    layoutType = layoutType
                )
            }
        }
    }
    Spacer(modifier = Modifier.width(16.dp))
    Image(
        modifier = Modifier.size(layoutType.calculateItemsRowMainIconSize()),
        painter = painterResource(id = model.typeIconName),
        contentDescription = null,
    )
}

@Composable
private fun ItemRowCentralPart(
    model: ItemCellModel,
    layoutType: CustomLayoutSize.LayoutType,
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel,
    rItem: RItem,
    showBottomDivider: Boolean
) {
    Column(modifier = Modifier.padding(start = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (model.title.isEmpty()) " " else model.title,
                    fontSize = layoutType.calculateItemsRowTextSize(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    var subtitleText = if (model.subtitle.isEmpty()) " " else model.subtitle
                    val shouldHideSubtitle =
                        model.subtitle.isEmpty() && (model.hasNote || !model.tagColors.isEmpty())
                    if (shouldHideSubtitle) {
                        subtitleText = ""
                    }
                    Text(
                        text = subtitleText,
                        fontSize = layoutType.calculateItemsRowTextSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = CustomPalette.LightCharcoal,
                    )
                    if (model.hasNote) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Image(
                            modifier = Modifier
                                .size(layoutType.calculateItemsRowNoteIconSize())
                                .align(Alignment.CenterVertically),
                            painter = painterResource(id = Drawables.item_note),
                            contentDescription = null,
                        )
                    }

                }
            }
            ItemRowRightPart(
                model = model,
                layoutType = layoutType,
                viewState = viewState,
                viewModel = viewModel,
                rItem = rItem
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (showBottomDivider) {
            CustomDivider()
        }
    }
}

@Composable
private fun RowScope.ItemRowRightPart(
    model: ItemCellModel,
    layoutType: CustomLayoutSize.LayoutType,
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel,
    rItem: RItem
) {
    SetAccessory(
        accessory = model.accessory,
        layoutType = layoutType
    )
    Spacer(modifier = Modifier.width(12.dp))
    AnimatedContent(
        modifier = Modifier.align(Alignment.CenterVertically),
        targetState = viewState.isEditing
    ) { isEditing ->
        if (!isEditing) {
            Row {
                Icon(
                    modifier = Modifier
                        .size(layoutType.calculateItemsRowAccessoryInfoIconSize())
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            onClick = {
                                viewModel.onAccessoryTapped(rItem)
                            }
                        ),
                    painter = painterResource(id = Drawables.accessory_icon),
                    contentDescription = null,
                    tint = CustomTheme.colors.zoteroBlueWithDarkMode
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun RowScope.SetAccessory(
    accessory: ItemCellModel.Accessory?,
    layoutType: CustomLayoutSize.LayoutType
) {
    if (accessory == null) {
        return
    }
    Spacer(modifier = Modifier.width(8.dp))
    when (accessory) {
        is ItemCellModel.Accessory.attachment -> {
            FileAttachmentView(
                modifier = Modifier
                    .size(layoutType.calculateItemsRowAccessoryIconSize())
                    .align(Alignment.CenterVertically),
                state = accessory.state,
                style = Style.list,
            )
        }

        is ItemCellModel.Accessory.doi, is ItemCellModel.Accessory.url -> {
            Image(
                modifier = Modifier
                    .size(layoutType.calculateItemsRowAccessoryIconSize())
                    .align(Alignment.CenterVertically),
                painter = painterResource(id = Drawables.list_link),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun ItemPlaceHolderRow(
    layoutType: CustomLayoutSize.LayoutType,
    showBottomDivider: Boolean = false
) {
    Row {
        Spacer(modifier = Modifier.width(16.dp))
        IconLoadingPlaceholder(
            modifier = Modifier
                .size(layoutType.calculateItemsRowMainIconSize())
                .align(Alignment.CenterVertically),
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    TextLoadingPlaceholder(
                        modifier = Modifier
                            .height(layoutType.calculateItemsRowPlaceholderSize())
                            .fillMaxWidth(0.8f),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        TextLoadingPlaceholder(
                            modifier = Modifier
                                .height(layoutType.calculateItemsRowPlaceholderSize())
                                .fillMaxWidth(0.4f),
                        )
                    }
                }
                IconLoadingPlaceholder(
                    modifier = Modifier
                        .size(layoutType.calculateItemsRowInfoIconSize())
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (showBottomDivider) {
                CustomDivider()
            }
        }
    }
}
