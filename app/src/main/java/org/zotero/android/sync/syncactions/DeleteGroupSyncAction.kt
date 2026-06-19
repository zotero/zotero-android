package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.DeleteGroupDbRequest

class DeleteGroupSyncAction @AssistedInject constructor(
    @Assisted("groupId") private val groupId: Int,

    private val dbWrapperMain: DbWrapperMain
) {
    fun result() {
        val request = DeleteGroupDbRequest(groupId = this.groupId)
        dbWrapperMain.realmDbStorage.perform(request = request)
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("groupId") groupId: Int): DeleteGroupSyncAction
    }
}