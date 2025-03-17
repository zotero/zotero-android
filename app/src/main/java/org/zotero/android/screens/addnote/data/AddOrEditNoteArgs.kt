package org.zotero.android.screens.addnote.data

import org.zotero.android.sync.LibraryIdentifier

data class AddOrEditNoteArgs(
    val title: TitleData?,
    val key: String,
    val libraryId: LibraryIdentifier,
    val readOnly: Boolean,
    val isFromDashboard: Boolean,
) {
    data class TitleData(
        val type: String,
        val title: String,
    )
}
