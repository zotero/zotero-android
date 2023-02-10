package org.zotero.android.database.objects

import org.zotero.android.sync.LibraryIdentifier

sealed class Conflict {
    data class groupRemoved(val groupId: Int, val groupName: String) : Conflict()
    data class groupWriteDenied(val groupId: Int, val groupName: String) : Conflict()
    data class objectsRemovedRemotely(
        val libraryId: LibraryIdentifier,
        val collections: List<String>,
        val items: List<String>,
        val searches: List<String>,
        val tags: List<String>
    ) : Conflict()

    data class removedItemsHaveLocalChanges(
        val keys: List<Pair<String, String>>,
        val libraryId: LibraryIdentifier
    ) : Conflict()
}