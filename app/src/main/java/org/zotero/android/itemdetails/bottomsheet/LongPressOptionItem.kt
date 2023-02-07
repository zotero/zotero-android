package org.zotero.android.itemdetails.bottomsheet

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import org.zotero.android.architecture.database.objects.Attachment
import org.zotero.android.dashboard.data.ItemDetailCreator
import org.zotero.android.sync.Note
import org.zotero.android.sync.Tag
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

sealed class LongPressOptionItem(
    @StringRes val titleId: Int,
    @DrawableRes val resIcon: Int,
    @ColorRes val textAndIconColor: Color? = null,
) {
    data class TrashNote(val note: Note): LongPressOptionItem(
        titleId = Strings.moveToTrash,
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

    data class RemoveDownloadAttachment(val attachment: Attachment): LongPressOptionItem(
        titleId = Strings.removeDownload,
        resIcon = Drawables.ic_delete_20dp
    )

    data class MoveToTrashAttachment(val attachment: Attachment): LongPressOptionItem(
        titleId = Strings.moveToTrash,
        textAndIconColor = CustomPalette.ErrorRed,
        resIcon = Drawables.ic_delete_20dp
    )
}