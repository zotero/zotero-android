package org.zotero.android.screens.itemdetails

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.Attachment
import org.zotero.android.helpers.formatter.dateFormatItemDetails
import org.zotero.android.screens.itemdetails.data.ItemDetailAttachmentKind
import org.zotero.android.screens.itemdetails.rows.ItemDetailsFieldRow
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.sync.Note
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.attachmentprogress.FileAttachmentView
import org.zotero.android.uicomponents.attachmentprogress.State
import org.zotero.android.uicomponents.attachmentprogress.Style
import org.zotero.android.uicomponents.foundation.debounceCombinedClickable
import org.zotero.android.uicomponents.foundation.safeClickable
import java.util.Date


@Composable
fun AddItemRow(
    titleRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(Drawables.add_circle_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier
                .weight(1f),
            text = stringResource(id = titleRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DatesRows(
    dateAdded: Date,
    dateModified: Date,
    layoutType: CustomLayoutSize.LayoutType,
) {
    ItemDetailsFieldRow(
        detailTitle = stringResource(id = Strings.date_added),
        detailValue = dateFormatItemDetails().format(dateAdded),
        layoutType = layoutType,
        onRowTapped = {
            //no action on tap, but still show ripple effect
        }
    )
    ItemDetailsFieldRow(
        stringResource(id = Strings.date_modified),
        dateFormatItemDetails().format(dateModified),
        layoutType,
        onRowTapped = {
            //no action on tap, but still show ripple effect
        }
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
    item {
        NewSettingsDivider()
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            ItemDetailHeaderSection(sectionTitle)
        }
    }
    itemsIndexed(
        itemTitles
    ) { index, item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .debounceCombinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = { onItemClicked(index) },
                    onLongClick = { onItemLongClicked(index) }
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = itemIcon),
                modifier = Modifier.size(28.dp),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                modifier = Modifier
                    .weight(1f),
                text = HtmlCompat.fromHtml(
                    item,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

@Composable
internal fun ItemDetailHeaderSection(
    sectionTitle: Int,
) {
    Column(modifier = Modifier.height(28.dp), verticalArrangement = Arrangement.Center) {
        Text(
            modifier = Modifier,
            text = stringResource(id = sectionTitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
        )
    }

}

private fun LazyListScope.listOfTags(
    layoutType: CustomLayoutSize.LayoutType,
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
) {
    item {
        NewSettingsDivider()
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            ItemDetailHeaderSection(Strings.item_detail_tags)
        }
    }
    items(
        viewState.tags
    ) { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .debounceCombinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = {},
                    onLongClick = { viewModel.onTagLongClick(item) }
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Drawables.tag),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier
                    .weight(1f),
                text = HtmlCompat.fromHtml(
                    item.name,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

    item {
        NewSettingsDivider()
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            ItemDetailHeaderSection(Strings.item_detail_attachments)
        }
    }
    items(
        items = viewState.attachments
    ) { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .debounceCombinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = { viewModel.openAttachment(item) },
                    onLongClick = { viewModel.onAttachmentLongClick(item) },
                ).padding(horizontal = 16.dp),
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
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                modifier = Modifier
                    .weight(1f),
                text = HtmlCompat.fromHtml(
                    item.title,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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


