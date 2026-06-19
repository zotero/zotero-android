package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.UpdateVersionType
import org.zotero.android.database.requests.UpdateVersionsDbRequest
import org.zotero.android.sync.LibraryIdentifier

class StoreVersionSyncAction @AssistedInject constructor(
    @Assisted("version") private val version: Int,
    @Assisted("type") private val type: UpdateVersionType,
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,

    private val dbWrapperMain: DbWrapperMain
) {
    fun result() {
        val request = UpdateVersionsDbRequest(
            version = this.version,
            libraryId = this.libraryId,
            type = this.type
        )
        dbWrapperMain.realmDbStorage.perform(request = request)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("version") version: Int,
            @Assisted("type") type: UpdateVersionType,
            @Assisted("libraryId") libraryId: LibraryIdentifier
        ): StoreVersionSyncAction
    }

}