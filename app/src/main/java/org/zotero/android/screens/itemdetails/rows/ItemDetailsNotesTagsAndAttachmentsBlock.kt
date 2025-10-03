package org.zotero.android.screens.itemdetails.rows

import androidx.compose.foundation.lazy.LazyListScope
import org.zotero.android.screens.itemdetails.ItemDetailsViewModel
import org.zotero.android.screens.itemdetails.ItemDetailsViewState
import org.zotero.android.screens.itemdetails.rows.edit.itemDetailsEditListOfTags
import org.zotero.android.sync.Note
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

internal fun LazyListScope.itemDetailsNotesTagsAndAttachmentsBlock(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
    onNoteClicked: (Note) -> Unit,
    onNoteLongClicked: (Note) -> Unit = {},
    onAddNote: () -> Unit,
) {
    if (!viewState.data.isAttachment) {
        itemDetailsListOfNotes(
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

    itemDetailsEditListOfTags(
        viewModel = viewModel,
        viewState = viewState,
    )

    itemDetailsListOfAttachments(
        viewState = viewState,
        viewModel = viewModel,
    )
}
