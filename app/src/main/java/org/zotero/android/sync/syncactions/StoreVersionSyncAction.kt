package org.zotero.android.sync.syncactions

import org.zotero.android.database.requests.UpdateVersionType
import org.zotero.android.database.requests.UpdateVersionsDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.syncactions.architecture.SyncAction

class StoreVersionSyncAction(
    val version: Int,
    val type: UpdateVersionType,
    val libraryId: LibraryIdentifier,
) : SyncAction() {
    fun result() {
        val request = UpdateVersionsDbRequest(
            version = this@StoreVersionSyncAction.version,
            libraryId = this@StoreVersionSyncAction.libraryId,
            type = this@StoreVersionSyncAction.type
        )
        dbWrapperMain.realmDbStorage.perform(request = request)
    }
}