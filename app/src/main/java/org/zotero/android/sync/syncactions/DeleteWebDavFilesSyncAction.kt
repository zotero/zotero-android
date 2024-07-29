package org.zotero.android.sync.syncactions

import org.zotero.android.api.network.CustomResult
import org.zotero.android.database.requests.DeleteWebDavDeletionsDbRequest
import org.zotero.android.database.requests.ReadWebDavDeletionsDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.syncactions.architecture.SyncAction
import timber.log.Timber

class DeleteWebDavFilesSyncAction(
    private val libraryId: LibraryIdentifier
) : SyncAction() {
    suspend fun result(): CustomResult<Set<String>> {
        val loadDeletionsResult = loadDeletions()
        if (loadDeletionsResult !is CustomResult.GeneralSuccess) {
            return loadDeletionsResult as CustomResult.GeneralError
        }
        val keys = loadDeletionsResult.value!!
        val deleteResult = webDavController.delete(keys=  keys)
        if (deleteResult !is CustomResult.GeneralSuccess) {
            return deleteResult as CustomResult.GeneralError
        }
        val result = deleteResult.value!!
        if (result.succeeded.isEmpty() && result.missing.isEmpty()) {
            return CustomResult.GeneralSuccess(result.failed)
        }
        val removeDeletionsResult = removeDeletions(keys = (result.succeeded.union(result.missing)))
        if (removeDeletionsResult !is CustomResult.GeneralSuccess) {
            return removeDeletionsResult as CustomResult.GeneralError
        }
        return CustomResult.GeneralSuccess(result.failed)
    }

    private fun loadDeletions(): CustomResult<List<String>> {
        try {
            val deletions =
                dbWrapperMain.realmDbStorage.perform(request = ReadWebDavDeletionsDbRequest(libraryId = this.libraryId))
            val keys = deletions.map { it.key }
            deletions.firstOrNull()?.realm?.refresh()
            return CustomResult.GeneralSuccess(keys)
        } catch (error: Exception) {
            Timber.e(error, "DeleteWebDavFilesSyncAction: could not read webdav deletions")
            return CustomResult.GeneralError.CodeError(error)
        }
    }

    private fun removeDeletions(keys: Set<String>): CustomResult<Unit> {
        try {
            dbWrapperMain.realmDbStorage.perform(
                request = DeleteWebDavDeletionsDbRequest(
                    keys = keys,
                    libraryId = this.libraryId
                )
            )
            return CustomResult.GeneralSuccess(Unit)
        } catch (error: Exception) {
            Timber.e(error, "DeleteWebDavFilesSyncAction: could not delete webdav deletions")
            return CustomResult.GeneralError.CodeError(error)
        }
    }
}