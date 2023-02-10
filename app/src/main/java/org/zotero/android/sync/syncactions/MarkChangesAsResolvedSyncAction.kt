package org.zotero.android.sync.syncactions

import org.zotero.android.database.DbWrapper
import org.zotero.android.database.requests.MarkAllLibraryObjectChangesAsSyncedDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncAction

class MarkChangesAsResolvedSyncAction(val libraryId: LibraryIdentifier, val dbWrapper: DbWrapper) :
    SyncAction<Unit> {

    override suspend fun result() {
        val request = MarkAllLibraryObjectChangesAsSyncedDbRequest(libraryId = this. libraryId)
        dbWrapper.realmDbStorage.perform(request = request)

    }
}