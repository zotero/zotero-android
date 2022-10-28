package org.zotero.android.sync.syncactions

import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.requests.DeleteGroupDbRequest
import org.zotero.android.sync.SyncAction

class DeleteGroupSyncAction(val groupId: Int, val dbWrapper: DbWrapper): SyncAction<Unit> {
    override suspend fun result() {
        val request = DeleteGroupDbRequest(groupId = this.groupId)
        dbWrapper.realmDbStorage.perform(request = request)
    }
}