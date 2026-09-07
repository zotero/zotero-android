package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.MarkAllLibraryObjectChangesAsSyncedDbRequest
import org.zotero.android.sync.LibraryIdentifier


class MarkChangesAsResolvedSyncAction @AssistedInject constructor(
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,

    private val dbWrapperMain: DbWrapperMain,
) {

    fun result() {
        val request = MarkAllLibraryObjectChangesAsSyncedDbRequest(libraryId = this.libraryId)
        dbWrapperMain.realmDbStorage.perform(request = request)

    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("libraryId") libraryId: LibraryIdentifier
        ): MarkChangesAsResolvedSyncAction
    }

}