package org.zotero.android.sync.syncactions

import org.zotero.android.database.requests.DeleteGroupDbRequest
import org.zotero.android.sync.syncactions.architecture.SyncAction

class DeleteGroupSyncAction(val groupId: Int): SyncAction() {
    fun result() {
        val request = DeleteGroupDbRequest(groupId = this.groupId)
        dbWrapper.realmDbStorage.perform(request = request)
    }
}