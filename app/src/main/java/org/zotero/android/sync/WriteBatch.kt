package org.zotero.android.sync

class WriteBatch(
    val libraryId: LibraryIdentifier,
    val objectS: SyncObject,
    val version: Int,
    val parameters: List<Map<String, Any>>,
) {
    companion object {
        val maxCount = 50

    }

    fun copy(version: Int): WriteBatch {
        return WriteBatch(
            libraryId = libraryId,
            objectS = objectS,
            version = version,
            parameters = parameters
        )
    }
}
