
package org.zotero.android.screens.itemdetails

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.Attachment
import org.zotero.android.helpers.formatter.dateFormatItemDetails
import org.zotero.android.screens.itemdetails.data.ItemDetailAttachmentKind
import org.zotero.android.sync.Note
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.attachmentprogress.FileAttachmentView
import org.zotero.android.uicomponents.attachmentprogress.State
import org.zotero.android.uicomponents.attachmentprogress.Style
import org.zotero.android.uicomponents.checkbox.CircleCheckBox
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.reorder.ReorderableState
import org.zotero.android.uicomponents.reorder.detectReorderAfterLongPress
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import java.util.Date

@Composable
internal fun FieldRow(
    detailTitle: String,
    detailValue: String,
    layoutType: CustomLayoutSize.LayoutType,
    textColor: Color = CustomTheme.colors.primaryContent,
    showDivider: Boolean,
    reorderState: ReorderableState ? = null,
    additionalInfoString: String? = null,
    onDelete: (() -> Unit)? = null,
    onRowTapped: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(
                onClick = onRowTapped,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            )
    ) {
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    modifier = Modifier
                        .size(28.dp)
                        .safeClickable(
                            onClick = onDelete,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false)
                        )
                        .padding(start = 4.dp),
                    painter = painterResource(id = Drawables.do_not_disturb_on_24px),
                    contentDescription = null,
                    tint = Color(0xFFDB2C3A)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.width(layoutType.calculateItemFieldLabelWidth())) {
                Text(
                    modifier = Modifier.align(Alignment.End),
                    text = detailTitle,
                    overflow =TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = CustomTheme.colors.secondaryContent,
                    style = CustomTheme.typography.newHeadline,
                )
            }

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    modifier = Modifier,
                    text = detailValue,
                    color = textColor,
                    style = CustomTheme.typography.newBody,
                )
            }
            if (additionalInfoString != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = additionalInfoString,
                    color = CustomTheme.colors.secondaryContent,
                    style = CustomTheme.typography.newBody,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (reorderState != null) {
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    modifier = Modifier
                        .size(28.dp)
                        .detectReorderAfterLongPress(reorderState),
                    painter = painterResource(id = Drawables.drag_handle_24px),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(CustomTheme.colors.reorderButtonColor),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (showDivider) {
            CustomDivider()
        }
    }
}

@Composable
fun AddItemRow(
    titleRes: Int,
    startPadding:Dp = 8.dp,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .padding(start = startPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.size(26.dp),
                painter = painterResource(id = Drawables.add_circle_24px),
                colorFilter = ColorFilter.tint(CustomTheme.colors.zoteroDefaultBlue),
                contentDescription = null,
            )

            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                text = stringResource(id = titleRes),
                style = CustomTheme.typography.newBody,
                color = CustomTheme.colors.zoteroDefaultBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DatesRows(
    dateAdded: Date,
    dateModified: Date,
    layoutType: CustomLayoutSize.LayoutType,
    showDivider: Boolean
) {
    FieldRow(
        detailTitle = stringResource(id = Strings.date_added),
        detailValue = dateFormatItemDetails().format(dateAdded),
        layoutType = layoutType,
        showDivider = showDivider,
    )
    FieldRow(
        stringResource(id = Strings.date_modified),
        dateFormatItemDetails().format(dateModified),
        layoutType,
        showDivider = showDivider,
    )
}

fun LazyListScope.notesTagsAndAttachmentsBlock(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
    layoutType: CustomLayoutSize.LayoutType,
    onNoteClicked: (Note) -> Unit,
    onNoteLongClicked: (Note) -> Unit = {},
    onAddNote: () -> Unit,
) {
    if (!viewState.data.isAttachment) {
        listOfNotes(
            sectionTitle = Strings.citation_notes,
            itemIcon = Drawables.item_type_note,
            itemTitles = viewState.notes.map { it.title },
            onItemClicked = {
                onNoteClicked(viewState.notes[it])
            },
            onItemLongClicked = {
                onNoteLongClicked(viewState.notes[it])
            },
            onAddItemClick = onAddNote,
            addTitleRes = Strings.item_detail_add_note
        )
    }

    listOfTags(
        layoutType = layoutType,
        viewModel = viewModel,
        viewState = viewState,
    )

    listOfAttachments(
        viewState = viewState,
        viewModel = viewModel,
    )
}

private fun LazyListScope.listOfNotes(
    sectionTitle: Int,
    @DrawableRes itemIcon: Int,
    itemTitles: List<String>,
    onItemClicked: (Int) -> Unit,
    onItemLongClicked: (Int) -> Unit,
    @StringRes addTitleRes: Int,
    onAddItemClick: (() -> Unit)? = null,
) {
    itemDetailHeaderSection(sectionTitle)
    itemsIndexed(
        itemTitles
    ) { index, item ->
        Column(modifier = Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = { onItemClicked(index) },
            onLongClick = { onItemLongClicked(index) }
        )) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = itemIcon),
                    modifier = Modifier.size(28.dp),
                    contentDescription = null,
                )

                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    text = HtmlCompat.fromHtml(
                        item,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ).toString(),
                    style = CustomTheme.typography.newBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            CustomDivider(modifier = Modifier.padding(start = 62.dp))
        }
    }
    if (onAddItemClick != null) {
        item {
            AddItemRow(
                titleRes = addTitleRes,
                onClick = onAddItemClick
            )
        }
    }

}

private fun LazyListScope.itemDetailHeaderSection(
    sectionTitle: Int,
) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = CustomTheme.colors.zoteroItemDetailSectionBackground)
        ) {
            CustomDivider()
            Text(
                modifier = Modifier.padding(12.dp).padding(start = 4.dp),
                text = stringResource(id = sectionTitle),
                color = CustomPalette.zoteroItemDetailSectionTitle,
                style = CustomTheme.typography.h6,
                fontSize = 17.sp,
            )
            CustomDivider()
        }
    }
}

private fun LazyListScope.listOfTags(
    layoutType: CustomLayoutSize.LayoutType,
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
) {
    itemDetailHeaderSection(Strings.item_detail_tags)
    items(
        viewState.tags
    ) { item ->
        Column(modifier = Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = {},
            onLongClick = { viewModel.onTagLongClick(item) }
        )) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleCheckBox(
                    isChecked = false,
                    layoutType = layoutType,
                    size = 28.dp
                )
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    text = HtmlCompat.fromHtml(
                        item.name,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ).toString(),
                    style = CustomTheme.typography.newBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            CustomDivider(modifier = Modifier.padding(start = 62.dp))
        }
    }
    item {
        AddItemRow(
            titleRes = Strings.item_detail_add_tag,
            onClick = viewModel::onAddTag
        )
    }
}

private fun LazyListScope.listOfAttachments(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
) {
    itemDetailHeaderSection(Strings.item_detail_attachments)
    items(
        items = viewState.attachments
    ) { item ->
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = { viewModel.openAttachment(item) },
                        onLongClick = { viewModel.onAttachmentLongClick(item) },
                    )
                    .padding(horizontal = 8.dp, vertical = 1.dp)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconSize = 28.dp
                val mainIconSize = 22.dp
                val badgeIconSize = 12.dp

                val type = viewModel.calculateAttachmentKind(attachment = item)
                val modifier = Modifier.size(iconSize)
                when (item.type) {
                    is Attachment.Kind.file -> {
                        when (type) {
                            ItemDetailAttachmentKind.default, ItemDetailAttachmentKind.disabled -> {
                                FileAttachmentView(
                                    modifier = modifier,
                                    state = State.ready(item.type),
                                    style = Style.detail,
                                    mainIconSize = mainIconSize,
                                    badgeIconSize = badgeIconSize,
                                )
                            }
                            is ItemDetailAttachmentKind.inProgress -> {
                                FileAttachmentView(
                                    modifier = modifier,
                                    state = State.progress(type.progressInHundreds),
                                    style = Style.detail,
                                    mainIconSize = mainIconSize,
                                    badgeIconSize = badgeIconSize,
                                )
                            }
                            is ItemDetailAttachmentKind.failed -> {
                                FileAttachmentView(
                                    modifier = modifier,
                                    state = State.failed(item.type, type.error),
                                    style = Style.detail,
                                    mainIconSize = mainIconSize,
                                    badgeIconSize = badgeIconSize,
                                )
                            }
                        }
                    }
                    is Attachment.Kind.url -> {
                        Image(
                            modifier = modifier,
                            painter = painterResource(id = Drawables.web_page),
                            contentDescription = null,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, top = 8.dp)
                ) {
                    Text(
                        text = HtmlCompat.fromHtml(
                            item.title,
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        ).toString(),
                        style = CustomTheme.typography.newBody,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CustomDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

    }
    if (!viewState.data.isAttachment) {
        item {
            AddItemRow(
                titleRes = Strings.item_detail_add_attachment,
                onClick = viewModel::onAddAttachment
            )
        }
    }

}


