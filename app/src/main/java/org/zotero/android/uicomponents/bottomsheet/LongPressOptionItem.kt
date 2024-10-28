package org.zotero.android.uicomponents.bottomsheet

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.RItem
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.Note
import org.zotero.android.sync.Tag
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette

sealed class LongPressOptionItem(
    @StringRes val titleId: Int,
    @DrawableRes val resIcon: Int? = null,
    val textAndIconColor: Color? = null,
    val isEnabled: Boolean = true,
) {
    data class TrashNote(val note: Note): LongPressOptionItem(
        titleId = Strings.move_to_trash,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.delete_24px
    )

    data class DeleteTag(val tag: Tag): LongPressOptionItem(
        titleId = Strings.delete,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.delete_24px
    )
    data class DeleteCreator(val creator: ItemDetailCreator): LongPressOptionItem(
        titleId = Strings.delete,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.delete_24px
    )

    data class DeleteAttachmentFile(val attachment: Attachment): LongPressOptionItem(
        titleId = Strings.item_detail_delete_attachment_file,
        resIcon = Drawables.delete_24px
    )

    data class MoveToTrashAttachment(val attachment: Attachment): LongPressOptionItem(
        titleId = Strings.move_to_trash,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.delete_24px
    )

    data class MoveToStandaloneAttachment(val attachment: Attachment): LongPressOptionItem(
        titleId = Strings.item_detail_move_to_standalone_attachment,
        resIcon = Drawables.vertical_align_top_24px
    )

    data class MoveToTrashItem(val item: RItem): LongPressOptionItem(
        titleId = Strings.move_to_trash,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.delete_24px
    )
    data class RemoveDownload(val item: RItem): LongPressOptionItem(
        titleId = Strings.items_action_remove_download,
        resIcon = Drawables.delete_24px
    )

    data class Download(val item: RItem): LongPressOptionItem(
        titleId = Strings.items_action_download,
        resIcon = Drawables.download_for_offline_24px
    )

    data class Duplicate(val item: RItem): LongPressOptionItem(
        titleId = Strings.items_action_duplicate,
        resIcon = Drawables.content_copy_24px
    )

    data class AddToCollection(val item: RItem): LongPressOptionItem(
        titleId = Strings.items_action_add_to_collection,
        resIcon = Drawables.create_new_folder_24px
    )

    data class RemoveFromCollection(val item: RItem): LongPressOptionItem(
        titleId = Strings.items_action_remove_from_collection,
        resIcon = Drawables.remove_from_collection
    )

    data class CreateParentItem(val item: RItem): LongPressOptionItem(
        titleId = Strings.items_action_create_parent,
        resIcon = Drawables.add_24px
    )

    data class TrashRestore(val item: RItem): LongPressOptionItem(
        titleId = Strings.restore,
        resIcon = Drawables.restore_trash
    )

    data class TrashDelete(val item: RItem): LongPressOptionItem(
        titleId = Strings.delete,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.delete_24px
    )

    data class CollectionDelete(val collection: org.zotero.android.sync.Collection) :
        LongPressOptionItem(
            titleId = Strings.delete,
            textAndIconColor = CustomPalette.ErrorRed,
            resIcon = Drawables.delete_24px
        )

    object CollectionEmptyTrash :
        LongPressOptionItem(
            titleId = Strings.collection_empty_trash,
            textAndIconColor = CustomPalette.ErrorRed,
            resIcon = Drawables.delete_24px
        )

    data class CollectionDownloadAttachments(
        val collectionId: CollectionIdentifier,
    ) :
        LongPressOptionItem(
            titleId = Strings.collections_download_attachments,
            resIcon = Drawables.baseline_download_24
        )

    data class CollectionRemoveDownloads(
        val collectionId: CollectionIdentifier,
    ) :
        LongPressOptionItem(
            titleId = Strings.collection_remove_downloads,
            resIcon = Drawables.remove_downloads_24
        )

    data class CollectionEdit(val collection: org.zotero.android.sync.Collection) :
        LongPressOptionItem(
            titleId = Strings.edit,
            resIcon = Drawables.edit_24px
        )

    data class CollectionNewSubCollection(val collection: org.zotero.android.sync.Collection) :
        LongPressOptionItem(
            titleId = Strings.collections_new_subcollection,
            resIcon = Drawables.create_new_folder_24px
        )

    object DisplayAllTagsInThisLibraryUnchecked: LongPressOptionItem(
        titleId = Strings.tag_picker_show_all,
    )

    object DisplayAllTagsInThisLibraryChecked: LongPressOptionItem(
        titleId = Strings.tag_picker_show_all,
        resIcon = Drawables.check_24px,
    )
}