package org.zotero.android.screens.share

import android.webkit.MimeTypeMap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import org.zotero.android.androidx.content.getDrawableByItemType
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.screens.share.data.CollectionPickerState
import org.zotero.android.screens.share.data.RecentData
import org.zotero.android.sync.Collection
import org.zotero.android.sync.Library
import org.zotero.android.sync.Tag
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.attachmentprogress.FileAttachmentView
import org.zotero.android.uicomponents.attachmentprogress.State
import org.zotero.android.uicomponents.attachmentprogress.Style
import org.zotero.android.uicomponents.theme.CustomTheme
import java.io.File


@Composable
internal fun ParsedShareItemSection(
    item: ItemResponse?,
    attachment: Pair<String, File>?,
    attachmentState: AttachmentState,
    title: String?,
    itemTitle: (item: ItemResponse, defaultValue: String) -> String,
) {
    Spacer(modifier = Modifier.height(20.dp))

    if (item == null && attachment == null) {
        if (attachmentState.translationInProgress || title == null) {
            return
        }
        ShareSection {
            SetItem(title = title, type = ItemTypes.webpage)
        }
        return
    }
    ShareSection {
        if (item != null && attachment != null) {
            val (attachmentTitle, file) = attachment
            val itemTitle = itemTitle(item, title ?: "")
            SetItem(title = itemTitle, type = item.rawType)
            SetAttachment(title = attachmentTitle, file = file, state = attachmentState, shouldAddExtraLeftPadding = true)
        } else if (item != null) {
            val title = itemTitle(item, title ?: "")
            SetItem(title = title, type = item.rawType)
        } else if (attachment != null) {
            val (title, file) = attachment
            SetAttachment(title = title, file = file, state = attachmentState, shouldAddExtraLeftPadding = false)
        }
    }

}

@Composable
private fun SetAttachment(title: String, file: File, state: AttachmentState, shouldAddExtraLeftPadding: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconSize = 28.dp
        val mainIconSize = 22.dp
        val badgeIconSize = 12.dp
        val contentType =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)!!
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
        Spacer(modifier = Modifier.width(16.dp))
        FileAttachmentView(
            modifier = Modifier.size(iconSize),
            state = attachmentState,
            style = Style.shareExtension,
            mainIconSize = mainIconSize,
            badgeIconSize = badgeIconSize,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = HtmlCompat.fromHtml(
                title,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            ).toString(),
            style = CustomTheme.typography.newBody,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.weight(1f))
        if (state is AttachmentState.downloading) {
            if (state.progress == 0) {
                CircularProgressIndicator(
                    color = CustomTheme.colors.zoteroDefaultBlue,
                    modifier = Modifier
                        .size(24.dp)
                )
            } else {
                CircularProgressIndicator(
                    progress = state.progress.toFloat(),
                    color = CustomTheme.colors.zoteroDefaultBlue,
                    modifier = Modifier
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}

@Composable
private fun SetItem(title: String, type: String) {
    ParsedShareItem(
        title = title,
        iconInt = LocalContext.current.getDrawableByItemType(
            ItemTypes.iconName(
                type,
                null
            )
        )
    )
}

@Composable
internal fun CollectionSection(
    navigateToMoreCollections: () -> Unit,
    onCollectionClicked: (collection: Collection?, library: Library) -> Unit,
    collectionPickerState: CollectionPickerState,
    recents: List<RecentData>,
) {
    ShareSectionTitle(
        titleId = Strings.shareext_collection_title
    )
    ShareSection {
        when (collectionPickerState) {
            CollectionPickerState.failed -> {
                ShareErrorItem(title = stringResource(id = Strings.shareext_sync_error))
            }

            CollectionPickerState.loading -> {
                ShareProgressItem(
                    title = stringResource(id = Strings.shareext_loading_collections),
                )
            }

            is CollectionPickerState.picked -> {
                val library = collectionPickerState.library
                val collection = collectionPickerState.collection
                recents.forEach { recent ->
                    val selected =
                        recent.collection?.identifier == collection?.identifier && recent.library.identifier == library.identifier
                    val title = recent.collection?.name ?: recent.library.name
                    ShareItem(
                        title = title,
                        onItemTapped = {
                            onCollectionClicked(
                                recent.collection,
                                recent.library
                            )
                        },
                        addCheckmarkIndicator = selected,
                    )
                }
                ShareItem(
                    title = stringResource(id = Strings.shareext_collection_other),
                    onItemTapped = navigateToMoreCollections,
                    textColor = CustomTheme.colors.zoteroDefaultBlue,
                    addNewScreenNavigationIndicator = true,
                )
            }
        }
    }
}

@Composable
internal fun TagsSection(navigateToTagPicker: () -> Unit, tags: List<Tag>) {
    ShareSectionTitle(titleId = Strings.shareext_tags_title)
    ShareSection {
        tags.forEach { tag ->
            ShareItem(
                title = tag.name,
                onItemTapped = {
                    //no-op
                },
            )
        }

        ShareItem(
            title = stringResource(id = Strings.add),
            onItemTapped = navigateToTagPicker,
            textColor = CustomTheme.colors.zoteroDefaultBlue,
            addNewScreenNavigationIndicator = true,
        )
    }
}