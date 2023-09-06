package org.zotero.android.uicomponents.bottomsheet

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.RItem
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.sync.Note
import org.zotero.android.sync.Tag
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette

sealed class LongPressOptionItem(
    @StringRes val titleId: Int,
    @DrawableRes val resIcon: Int? = null,
    val textAndIconColor: Color? = null,
) {
    data class TrashNote(val note: Note): LongPressOptionItem(
        titleId = Strings.move_to_trash,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.ic_delete_20dp
    )

    data class DeleteTag(val tag: Tag): LongPressOptionItem(
        titleId = Strings.delete,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.ic_delete_20dp
    )
    data class DeleteCreator(val creator: ItemDetailCreator): LongPressOptionItem(
        titleId = Strings.delete,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.ic_delete_20dp
    )

    data class DeleteAttachmentFile(val attachment: Attachment): LongPressOptionItem(
        titleId = Strings.item_detail_delete_attachment_file,
        resIcon = Drawables.ic_delete_20dp
    )

    data class MoveToTrashAttachment(val attachment: Attachment): LongPressOptionItem(
        titleId = Strings.move_to_trash,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.ic_delete_20dp
    )

    data class MoveToStandaloneAttachment(val attachment: Attachment): LongPressOptionItem(
        titleId = Strings.item_detail_move_to_standalone_attachment,
        resIcon = Drawables.baseline_arrow_upward_24
    )

    data class MoveToTrashItem(val item: RItem): LongPressOptionItem(
        titleId = Strings.move_to_trash,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.ic_delete_20dp
    )
    data class RemoveDownload(val item: RItem): LongPressOptionItem(
        titleId = Strings.items_action_remove_download,
        resIcon = Drawables.ic_delete_20dp
    )

    data class Download(val item: RItem): LongPressOptionItem(
        titleId = Strings.items_action_download,
        resIcon = Drawables.baseline_download_for_offline_24
    )

    data class Duplicate(val item: RItem): LongPressOptionItem(
        titleId = Strings.items_action_duplicate,
        resIcon = Drawables.baseline_content_copy_24
    )

    data class CreateParentItem(val item: RItem): LongPressOptionItem(
        titleId = Strings.items_action_create_parent,
        resIcon = Drawables.baseline_add_24
    )

    data class TrashRestore(val item: RItem): LongPressOptionItem(
        titleId = Strings.restore,
        resIcon = Drawables.restore_trash
    )

    data class TrashDelete(val item: RItem): LongPressOptionItem(
        titleId = Strings.delete,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.empty_trash
    )

    data class CollectionDelete(val collection: org.zotero.android.sync.Collection) :
        LongPressOptionItem(
            titleId = Strings.delete,
            textAndIconColor = CustomPalette.ErrorRed,
            resIcon = Drawables.empty_trash
        )

    data class CollectionEdit(val collection: org.zotero.android.sync.Collection) :
        LongPressOptionItem(
            titleId = Strings.edit,
            resIcon = Drawables.baseline_edit_24
        )

    data class CollectionNewSubCollection(val collection: org.zotero.android.sync.Collection) :
        LongPressOptionItem(
            titleId = Strings.collections_new_subcollection,
            resIcon = Drawables.baseline_create_new_folder_24
        )


    object DeselectAll: LongPressOptionItem(
        titleId = Strings.items_deselect_all,
    )

    object ShowAutomaticTags: LongPressOptionItem(
        titleId = Strings.tag_picker_show_auto,
    )
    object DeleteAutomaticTags: LongPressOptionItem(
        titleId = Strings.tag_picker_delete_automatic,
        textAndIconColor = CustomPalette.ErrorRed,
    )
}