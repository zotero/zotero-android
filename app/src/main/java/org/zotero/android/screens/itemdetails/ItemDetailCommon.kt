
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
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
            indication = rememberRipple(bounded = true)
        )
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    modifier = Modifier
                        .size(layoutType.calculateIconSize())
                        .safeClickable(
                            onClick = onDelete,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false)
                        )
                        .padding(start = 4.dp),
                    painter = painterResource(id = Drawables.ic_delete_20dp),
                    contentDescription = null,
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
                    style = CustomTheme.typography.default,
                    fontSize = layoutType.calculateTextSize(),
                )
            }

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    modifier = Modifier,
                    text = detailValue,
                    color = textColor,
                    style = CustomTheme.typography.default,
                    fontSize = layoutType.calculateTextSize(),
                )
            }
            if (additionalInfoString != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = additionalInfoString,
                    color = CustomTheme.colors.secondaryContent,
                    style = CustomTheme.typography.default,
                    fontSize = layoutType.calculateTextSize(),
                )
                Spacer(modifier = Modifier.width(8.dp))
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
    onClick: () -> Unit,
    layoutType: CustomLayoutSize.LayoutType
) {
    Column(
        modifier = Modifier
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.size(layoutType.calculateIconSize()),
                painter = painterResource(id = Drawables.add_icon),
                contentDescription = null,
            )

            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                text = stringResource(id = titleRes),
                fontSize = layoutType.calculateTextSize(),
                color = CustomTheme.colors.zoteroBlueWithDarkMode,
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
        listOfItems(
            sectionTitle = Strings.notes,
            itemIcon = Drawables.item_note,
            itemTitles = viewState.notes.map { it.title },
            layoutType = layoutType,
            onItemClicked = {
                onNoteClicked(viewState.notes[it])
            },
            onItemLongClicked = {
                onNoteLongClicked(viewState.notes[it])
            },
            onAddItemClick = onAddNote,
            addTitleRes = Strings.add_note
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
        layoutType = layoutType,
    )
}

private fun LazyListScope.listOfItems(
    sectionTitle: Int,
    @DrawableRes itemIcon: Int,
    itemTitles: List<String>,
    onItemClicked: (Int) -> Unit,
    onItemLongClicked: (Int) -> Unit,
    @StringRes addTitleRes: Int,
    onAddItemClick: (() -> Unit)? = null,
    layoutType: CustomLayoutSize.LayoutType
) {
    itemDetailHeaderSection(sectionTitle, layoutType)
    itemsIndexed(
        itemTitles
    ) { index, item ->
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(),
                        onClick = { onItemClicked(index) },
                        onLongClick = { onItemLongClicked(index) }
                    )
                    .padding(all = 8.dp)
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = itemIcon),
                    modifier = Modifier.size(layoutType.calculateIconSize()),
                    contentDescription = null,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, top = 8.dp)
                ) {
                    Text(
                        text = HtmlCompat.fromHtml(
                            item,
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        ).toString(),
                        fontSize = layoutType.calculateTextSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CustomDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

    }
    if (onAddItemClick != null) {
        item {
            AddItemRow(
                layoutType = layoutType,
                titleRes = addTitleRes,
                onClick = onAddItemClick
            )
        }
    }

}

private fun LazyListScope.itemDetailHeaderSection(
    sectionTitle: Int,
    layoutType: CustomLayoutSize.LayoutType
) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = CustomTheme.colors.zoteroItemDetailSectionBackground)
        ) {
            CustomDivider()
            Text(
                modifier = Modifier.padding(12.dp),
                text = stringResource(id = sectionTitle),
                color = CustomPalette.zoteroItemDetailSectionTitle,
                style = CustomTheme.typography.h6,
                fontSize = layoutType.calculateTextSize(),
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
    itemDetailHeaderSection(Strings.tags, layoutType)
    items(
        viewState.tags
    ) { item ->
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(),
                        onClick = {},
                        onLongClick = { viewModel.onTagLongClick(item) }
                    )
                    .padding(all = 8.dp)
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleCheckBox(
                    isChecked = false,
                    layoutType = layoutType
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, top = 8.dp)
                ) {
                    Text(
                        text = HtmlCompat.fromHtml(
                            item.name,
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        ).toString(),
                        fontSize = layoutType.calculateTextSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CustomDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

    }
    item {
        AddItemRow(
            layoutType = layoutType,
            titleRes = Strings.add_tag,
            onClick = viewModel::onAddTag
        )
    }
}

private fun LazyListScope.listOfAttachments(
    layoutType: CustomLayoutSize.LayoutType,
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
) {
    itemDetailHeaderSection(Strings.attachments, layoutType)
    items(
        items = viewState.attachments
    ) { item ->
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(),
                        onClick = { viewModel.openAttachment(item) },
                        onLongClick = { viewModel.onAttachmentLongClick(item) },
                    )
                    .padding(all = 8.dp)
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val type = viewModel.calculateAttachmentKind(attachment = item)
                when (item.type) {
                    is Attachment.Kind.file -> {
                        when (type) {
                            ItemDetailAttachmentKind.default, ItemDetailAttachmentKind.disabled -> {
                                FileAttachmentView(
                                    modifier = Modifier.size(layoutType.calculateIconSize()),
                                    state = State.ready(item.type),
                                    style = Style.detail,
                                )
                            }
                            is ItemDetailAttachmentKind.inProgress -> {
                                FileAttachmentView(
                                    modifier = Modifier.size(layoutType.calculateIconSize()),
                                    state = State.progress(type.progressInHundreds),
                                    style = Style.detail,
                                )
                            }
                            is ItemDetailAttachmentKind.failed -> {
                                FileAttachmentView(
                                    modifier = Modifier.size(layoutType.calculateIconSize()),
                                    state = State.failed(item.type, type.error),
                                    style = Style.detail,
                                )
                            }
                        }
                    }
                    is Attachment.Kind.url -> {
                        Image(
                            modifier = Modifier.size(layoutType.calculateIconSize()),
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
                        fontSize = layoutType.calculateTextSize(),
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
                layoutType = layoutType,
                titleRes = Strings.add_attachment,
                onClick = viewModel::onAddAttachment
            )
        }
    }

}


