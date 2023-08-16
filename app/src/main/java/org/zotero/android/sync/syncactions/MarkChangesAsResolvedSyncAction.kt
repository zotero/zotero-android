package org.zotero.android.sync.syncactions

import org.zotero.android.database.requests.MarkAllLibraryObjectChangesAsSyncedDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.syncactions.architecture.SyncAction


class MarkChangesAsResolvedSyncAction(val libraryId: LibraryIdentifier) : SyncAction() {

    fun result() {
        val request = MarkAllLibraryObjectChangesAsSyncedDbRequest(libraryId = this.libraryId)
        dbWrapper.realmDbStorage.perform(request = request)

    }
}