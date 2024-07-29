package org.zotero.android.sync.syncactions

import org.zotero.android.database.requests.MarkGroupForResyncDbAction
import org.zotero.android.sync.syncactions.architecture.SyncAction


class MarkGroupForResyncSyncAction(
    val identifier: Int,
) : SyncAction() {
    fun result() {
        dbWrapperMain.realmDbStorage.perform(request = MarkGroupForResyncDbAction(identifier = this.identifier))

    }
}