package org.zotero.android.dashboard.data

import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag

data class AddOrEditNoteArgs(
    val title: TitleData?,
    val key: String,
    val libraryId: LibraryIdentifier,
    val readOnly: Boolean,
    var text: String,
    var tags: List<Tag>,
    val isFromDashboard: Boolean,
) {
    data class TitleData(
        val type: String,
        val title: String,
    )
}
