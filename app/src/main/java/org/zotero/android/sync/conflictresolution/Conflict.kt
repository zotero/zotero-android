package org.zotero.android.sync.conflictresolution

import org.zotero.android.sync.LibraryIdentifier

sealed class Conflict {
    data class groupRemoved(val groupId: Int, val name: String) : Conflict()
    data class groupMetadataWriteDenied(val groupId: Int, val name: String) : Conflict()
    data class groupFileWriteDenied(val groupId: Int, val name: String) : Conflict()
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