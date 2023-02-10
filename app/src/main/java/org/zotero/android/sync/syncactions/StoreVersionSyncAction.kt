package org.zotero.android.sync.syncactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.requests.UpdateVersionType
import org.zotero.android.database.requests.UpdateVersionsDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncAction

class StoreVersionSyncAction(
    val dbWrapper: DbWrapper,
    val version: Int,
    val type: UpdateVersionType,
    val libraryId: LibraryIdentifier,
) : SyncAction<Unit> {
    override suspend fun result() = withContext(Dispatchers.IO) {
        val request = UpdateVersionsDbRequest(
            version = this@StoreVersionSyncAction.version,
            libraryId = this@StoreVersionSyncAction.libraryId,
            type = this@StoreVersionSyncAction.type
        )
        dbWrapper.realmDbStorage.perform(request = request)
    }
}