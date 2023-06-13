package org.zotero.android.sync.syncactions

import org.zotero.android.database.DbWrapper
import org.zotero.android.database.requests.MarkGroupForResyncDbAction
import org.zotero.android.sync.SyncAction

class MarkGroupForResyncSyncAction(
    val dbStorage: DbWrapper,
    val identifier: Int,
) : SyncAction<Unit> {
    override suspend fun result() {
        dbStorage.realmDbStorage.perform(request = MarkGroupForResyncDbAction(identifier = this.identifier))

    }
}