package org.zotero.android.sync.syncactions

import org.zotero.android.api.network.CustomResult
import org.zotero.android.database.requests.MarkObjectsAsChangedByUser
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.syncactions.architecture.SyncAction

class RestoreDeletionsSyncAction(
    val libraryId: LibraryIdentifier,
    val collections: List<String>,
    val items: List<String>,
) : SyncAction() {

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

}