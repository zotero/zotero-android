package org.zotero.android.sync.syncactions

import org.zotero.android.database.requests.MarkGroupAsLocalOnlyDbRequest
import org.zotero.android.sync.syncactions.architecture.SyncAction


class MarkGroupAsLocalOnlySyncAction(
    val groupId: Int,
) : SyncAction() {
    fun result() {
        val request = MarkGroupAsLocalOnlyDbRequest(groupId = this.groupId)
        dbWrapperMain.realmDbStorage.perform(request = request)
    }

}