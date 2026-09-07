package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.api.network.CustomResult
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.MarkObjectsAsChangedByUser
import org.zotero.android.sync.LibraryIdentifier

class RestoreDeletionsSyncAction @AssistedInject constructor(
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,
    @Assisted("collections") private val collections: List<String>,
    @Assisted("items") private val items: List<String>,

    private val dbWrapperMain: DbWrapperMain,
) {

    fun result(): CustomResult<Unit> {
        try {
            val request = MarkObjectsAsChangedByUser(
                libraryId = this.libraryId,
                collections = this.collections,
                items = this.items
            )
            dbWrapperMain.realmDbStorage.perform(request = request)
            return CustomResult.GeneralSuccess(Unit)
        } catch (error: Throwable) {
            return CustomResult.GeneralError.CodeError(error)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("libraryId") libraryId: LibraryIdentifier,
            @Assisted("collections") collections: List<String>,
            @Assisted("items") items: List<String>
        ): RestoreDeletionsSyncAction
    }

}