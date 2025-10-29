package org.zotero.android.screens.share.sections

import android.webkit.MimeTypeMap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import org.zotero.android.androidx.content.getDrawableByItemType
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.screens.share.rows.ShareParsedItemRow
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.uicomponents.attachmentprogress.FileAttachmentView
import org.zotero.android.uicomponents.attachmentprogress.State
import org.zotero.android.uicomponents.attachmentprogress.Style
import java.io.File

@Composable
internal fun ShareParsedItemSection(
    item: ItemResponse?,
    attachment: Pair<String, File>?,
    attachmentState: AttachmentState,
    title: String?,
    itemTitle: (item: ItemResponse, defaultValue: String) -> String,
) {
    if (item == null && attachment == null) {
        if (attachmentState.translationInProgress || title == null) {
            return
        }
        ShareSetItem(title = title, type = ItemTypes.webpage)
        return
    }
    if (item != null && attachment != null) {
        val (attachmentTitle, file) = attachment
        val itemTitle = itemTitle(item, title ?: "")
        ShareSetItem(title = itemTitle, type = item.rawType)
        ShareSetAttachment(
            title = attachmentTitle,
            file = file,
            state = attachmentState,
            shouldAddExtraLeftPadding = true
        )
    } else if (item != null) {
        val title = itemTitle(item, title ?: "")
        ShareSetItem(title = title, type = item.rawType)
    } else if (attachment != null) {
        val (title, file) = attachment
        ShareSetAttachment(
            title = title,
            file = file,
            state = attachmentState,
            shouldAddExtraLeftPadding = false
        )
    }

}

@Composable
internal fun ShareSetAttachment(
    title: String,
    file: File,
    state: AttachmentState,
    shouldAddExtraLeftPadding: Boolean
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconSize = 28.dp
        val mainIconSize = 22.dp
        val badgeIconSize = 12.dp
        val contentType =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                ?: "text/html" //TODO proper solution
        val attachmentState = State.stateFrom(
            type = Attachment.Kind.file(
                filename = "",
                contentType = contentType,
                location = Attachment.FileLocation.local,
                linkType = Attachment.FileLinkType.importedFile
            ), progress = null, error = state.error
        )
        if (shouldAddExtraLeftPadding) {
            Spacer(modifier = Modifier.width(16.dp))
        }
        FileAttachmentView(
            modifier = Modifier.size(iconSize),
            state = attachmentState,
            style = Style.shareExtension,
            mainIconSize = mainIconSize,
            badgeIconSize = badgeIconSize,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = HtmlCompat.fromHtml(
                title,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            ).toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.weight(1f))
        if (state is AttachmentState.downloading) {
            if (state.progress == 0) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                )
            } else {
                CircularProgressIndicator(
                    progress = { state.progress.toFloat() },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    }
}

@Composable
internal fun ShareSetItem(title: String, type: String) {
    ShareParsedItemRow(
        title = title,
        iconInt = LocalContext.current.getDrawableByItemType(
            ItemTypes.iconName(
                type,
                null
            )
        )
    )
}
