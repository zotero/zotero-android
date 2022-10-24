package org.zotero.android.sync.syncactions

import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.requests.UpdateVersionType
import org.zotero.android.architecture.database.requests.UpdateVersionsDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncAction

class StoreVersionSyncAction(
    val dbWrapper: DbWrapper,
    val version: Int,
    val type: UpdateVersionType,
    val libraryId: LibraryIdentifier,
) : SyncAction<Unit> {
    override suspend fun result() {
        val request = UpdateVersionsDbRequest(version = this. version, libraryId = this.libraryId, type = this.type)
        dbWrapper.realmDbStorage.perform(request = request)
    }
}