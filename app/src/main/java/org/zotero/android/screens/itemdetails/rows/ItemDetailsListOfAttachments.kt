package org.zotero.android.screens.itemdetails.rows

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import org.zotero.android.database.objects.Attachment
import org.zotero.android.screens.itemdetails.AddItemRow
import org.zotero.android.screens.itemdetails.ItemDetailHeaderSection
import org.zotero.android.screens.itemdetails.ItemDetailsViewModel
import org.zotero.android.screens.itemdetails.ItemDetailsViewState
import org.zotero.android.screens.itemdetails.data.ItemDetailAttachmentKind
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.attachmentprogress.FileAttachmentView
import org.zotero.android.uicomponents.attachmentprogress.State
import org.zotero.android.uicomponents.attachmentprogress.Style
import org.zotero.android.uicomponents.foundation.debounceCombinedClickable

internal fun LazyListScope.itemDetailsListOfAttachments(
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
