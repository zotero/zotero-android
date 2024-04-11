package org.zotero.android.screens.share

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.screens.share.data.CollectionPickerState
import org.zotero.android.screens.share.data.RecentData
import org.zotero.android.sync.Collection
import org.zotero.android.sync.Library
import org.zotero.android.sync.Tag
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.uicomponents.Strings
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
    if (item == null && attachment == null) {
        if (attachmentState.translationInProgress || title == null) {
            val progress =
                if (attachmentState is AttachmentState.downloading) {
                    attachmentState.progress
                } else {
                    0
                }
            ShareProgressItem(title = "Parsing Shared Item", progress = progress)
            return
        }
        SetItem(title = title, type = ItemTypes.webpage)
        return
    }
    //TODO show either title + icon view or FileAttachmentView

    if (item != null && attachment != null) {
        val (attachmentTitle, file) = attachment
        val itemTitle = itemTitle(item, title ?: "")
        SetItem(title = itemTitle, type = item.rawType)
        //TODO show attachment
    } else if (item != null) {
        val title = itemTitle(item, title ?: "")
        SetItem(title = title, type = item.rawType)
    } else if (attachment != null) {
        val (title, file) = attachment
        //TODO display in FileAttachmentView
        SetItem(title = title, type = ItemTypes.document)
    }
}

@Composable
private fun SetItem(title: String, type: String) {
    Spacer(modifier = Modifier.height(20.dp))
    ShareSection {
        ParsedShareItem(
            title = title,
            iconInt = ItemTypes.iconName(type, null)
        )
    }
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
                    progress = 0
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