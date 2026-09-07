package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.MarkGroupForResyncDbAction

class MarkGroupForResyncSyncAction @AssistedInject constructor(
    @Assisted("identifier") private val identifier: Int,

    private val dbWrapperMain: DbWrapperMain,
) {
    fun result() {
        dbWrapperMain.realmDbStorage.perform(request = MarkGroupForResyncDbAction(identifier = this.identifier))
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("identifier") identifier: Int): MarkGroupForResyncSyncAction
    }
}