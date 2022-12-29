package org.zotero.android.dashboard.data

import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag
import org.zotero.android.uidata.Collection

data class AddOrEditNoteArgs(
    val title: TitleData?,
    val key: String,
    val libraryId: LibraryIdentifier,
    val readOnly: Boolean,
    var text: String,
    var tags: List<Tag>,
    val collection: Collection = Collection(identifier = CollectionIdentifier.collection(""), name = "", itemCount = 0),
    val library: Library = Library(LibraryIdentifier.group(0),"",false, false),
) {
    data class TitleData(
        val type: String,
        val title: String,
    )
}
