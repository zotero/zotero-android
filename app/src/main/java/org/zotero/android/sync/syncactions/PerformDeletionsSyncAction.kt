package org.zotero.android.sync.syncactions

import org.zotero.android.database.requests.PerformDeletionsDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.syncactions.architecture.SyncAction


class PerformDeletionsSyncAction(
    val libraryId: LibraryIdentifier,
    val collections: List<String>,
    val items: List<String>,
    val searches: List<String>,
    val tags: List<String>,
    val conflictMode: PerformDeletionsDbRequest.ConflictResolutionMode,
) : SyncAction() {
    fun result(): List<Pair<String, String>> {
        val request = PerformDeletionsDbRequest(
            libraryId = this.libraryId,
            collections = this.collections,
            items = this.items,
            searches = this.searches,
            tags = this.tags,
            conflictMode = this.conflictMode
        )
        val conflicts = dbWrapperMain.realmDbStorage.perform(request = request, invalidateRealm = true)
        return conflicts
    }
}