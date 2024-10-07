package org.zotero.android.sync.conflictresolution

import org.zotero.android.sync.LibraryIdentifier

sealed class ConflictResolution {
    data class deleteGroup(val id: Int) : ConflictResolution()
    data class markGroupAsLocalOnly(val id: Int) : ConflictResolution()
    data class revertGroupChanges(val id: LibraryIdentifier) : ConflictResolution()
    data class revertGroupFiles(val id: LibraryIdentifier): ConflictResolution()
    data class skipGroup(val id: LibraryIdentifier): ConflictResolution()
    data class remoteDeletionOfActiveObject(
        val libraryId: LibraryIdentifier,
        val toDeleteCollections: List<String>,
        val toRestoreCollections: List<String>,
        val toDeleteItems: List<String>,
        val toRestoreItems: List<String>,
        val searches: List<String>,
        val tags: List<String>
    ) : ConflictResolution()

    data class remoteDeletionOfChangedItem(
        val libraryId: LibraryIdentifier,
        val toDelete: List<String>,
        val toRestore: List<String>
    ) : ConflictResolution()
}