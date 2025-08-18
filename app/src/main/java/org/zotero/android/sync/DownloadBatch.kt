package org.zotero.android.sync

class DownloadBatch(
    val libraryId: LibraryIdentifier,
    val objectS: SyncObject,
    val keys: List<String>,
    val version: Int
) {

    companion object {
        const val maxCount = 50
    }

    override fun equals(other: Any?): Boolean {
        return true
    }

    override fun hashCode(): Int {
        var result = libraryId.hashCode()
        result = 31 * result + objectS.hashCode()
        result = 31 * result + keys.hashCode()
        result = 31 * result + version
        return result
    }
}
