package org.zotero.android.screens.addbyidentifier.ui

import androidx.compose.foundation.Image
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import org.zotero.android.androidx.content.getDrawableByItemType
import org.zotero.android.attachmentdownloader.RemoteAttachmentDownloader
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.sync.SyncError
import org.zotero.android.uicomponents.Strings
import org.zotero.android.screens.addbyidentifier.data.LookupRow
import org.zotero.android.uicomponents.attachmentprogress.FileAttachmentView
import org.zotero.android.uicomponents.attachmentprogress.State
import org.zotero.android.uicomponents.attachmentprogress.Style
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

internal fun LazyListScope.addByIdentifierTable(rows: List<LookupRow>) {
    rows.forEach { row ->
        item {
            when (row) {
                is LookupRow.item -> {
                    val data = row.item
                    LookupItemRow(
                        title = data.title,
                        type = data.type
                    )
                }

                is LookupRow.attachment -> {
                    val attachment = row.attachment
                    val update = row.updateKind
                    LookupAttachmentRow(
                        title = attachment.title,
                        attachmentType = attachment.type,
                        update = update
                    )
                }

                is LookupRow.identifier -> {
                    val identifier = row.identifier
                    val state = row.state
                    LookupIdentifierRow(
                        title = identifier,
                        state = state,
                    )
                }
            }
        }
    }
}

@Composable
internal fun LookupItemRow(
    title: String, type: String
) {
    val iconSize = 28.dp

    val modifier = Modifier.size(iconSize)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = modifier,
            painter = painterResource(
                id = LocalContext.current.getDrawableByItemType(
                    ItemTypes.iconName(
                        type,
                        null
                    )
                )
            ),
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, top = 8.dp)
        ) {
            Text(
                text = HtmlCompat.fromHtml(
                    title,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toString(),
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.newBody,
            )
            NewDivider(modifier = Modifier.padding(top = 8.dp))
        }
    }
}


@Composable
internal fun LookupAttachmentRow(
    title: String,
    attachmentType: Attachment.Kind,
    update: RemoteAttachmentDownloader.Update.Kind,
) {
    val iconSize = 28.dp
    val mainIconSize = 22.dp
    val badgeIconSize = 12.dp
    val modifier = Modifier.size(iconSize)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(30.dp))
        when (update) {
            is RemoteAttachmentDownloader.Update.Kind.ready -> {
                FileAttachmentView(
                    modifier = modifier,
                    state = State.ready(attachmentType),
                    style = Style.lookup,
                    mainIconSize = mainIconSize,
                    badgeIconSize = badgeIconSize,
                )
            }

            is RemoteAttachmentDownloader.Update.Kind.progress -> {
                FileAttachmentView(
                    modifier = modifier,
                    state = State.progress(update.progressInHundreds),
                    style = Style.lookup,
                    mainIconSize = mainIconSize,
                    badgeIconSize = badgeIconSize,
                )
            }

            is RemoteAttachmentDownloader.Update.Kind.failed, is RemoteAttachmentDownloader.Update.Kind.cancelled -> {
                FileAttachmentView(
                    modifier = modifier,
                    state = State.failed(attachmentType, SyncError.NonFatal.unchanged),
                    style = Style.lookup,
                    mainIconSize = mainIconSize,
                    badgeIconSize = badgeIconSize,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, top = 8.dp)
        ) {
            Text(
                text = HtmlCompat.fromHtml(
                    title,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toString(),
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.newBody,
            )
            NewDivider(modifier = Modifier.padding(top = 8.dp))
        }
    }

}

@Composable
internal fun LookupIdentifierRow(
    title: String,
    state: LookupRow.IdentifierState,
) {
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(44.dp)
        ) {

            Text(
                modifier = Modifier.weight(1f),
                text = title,
                maxLines = 1,
                color = CustomTheme.colors.primaryContent,
                overflow = TextOverflow.Ellipsis,
                style = CustomTheme.typography.newBody,
            )

            Spacer(modifier = Modifier.width(16.dp))

            when (state) {
                LookupRow.IdentifierState.enqueued -> {
                    Text(
                        text = stringResource(id = Strings.scan_barcode_item_queued_state),
                        color = CustomPalette.SystemGray,
                        maxLines = 1,
                        style = CustomTheme.typography.newBody,
                    )
                }

                LookupRow.IdentifierState.inProgress -> {
                    CircularProgressIndicator(
                        color = CustomTheme.colors.zoteroDefaultBlue,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }

                LookupRow.IdentifierState.failed -> {
                    Text(
                        text = stringResource(id = Strings.scan_barcode_item_failed_state),
                        color = CustomPalette.ErrorRed,
                        maxLines = 1,
                        style = CustomTheme.typography.newBody,
                    )
                }
            }
        }
        NewDivider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )
    }

}