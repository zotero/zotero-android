package org.zotero.android.sync

class DeleteBatch(
    val libraryId: LibraryIdentifier,
    val objectS: SyncObject,
    val version: Int,
    val keys: List<String>,
) {

    companion object {
        const val maxCount: Int = 50

    }


    fun copy(version: Int): DeleteBatch {
        return DeleteBatch(libraryId = libraryId, objectS = objectS, version = version, keys = keys)
    }
}
