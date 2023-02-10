package org.zotero.android.sync.syncactions

import org.zotero.android.database.DbWrapper
import org.zotero.android.database.requests.MarkGroupAsLocalOnlyDbRequest
import org.zotero.android.sync.SyncAction

class MarkGroupAsLocalOnlySyncAction(
    val groupId: Int,
    val dbWrapper: DbWrapper
): SyncAction<Unit> {
    override suspend fun result() {
        val request = MarkGroupAsLocalOnlyDbRequest(groupId = this.groupId)
        dbWrapper.realmDbStorage.perform(request = request)
    }

}