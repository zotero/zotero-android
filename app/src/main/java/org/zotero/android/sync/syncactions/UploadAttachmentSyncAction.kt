package org.zotero.android.sync.syncactions

import org.zotero.android.api.SyncApi
import org.zotero.android.architecture.database.DbRequest
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.requests.CheckItemIsChangedDbRequest
import org.zotero.android.architecture.database.requests.MarkAttachmentUploadedDbRequest
import org.zotero.android.architecture.database.requests.ReadItemDbRequest
import org.zotero.android.architecture.database.requests.UpdateVersionType
import org.zotero.android.architecture.database.requests.UpdateVersionsDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SyncAction
import org.zotero.android.sync.SyncActionError
import org.zotero.android.sync.SyncObject
import timber.log.Timber
import java.io.File

class UploadAttachmentSyncAction(
    val key: String,
    val file: File,
    val filename: String,
    val md5: String,
    val mtime: Int,
    val libraryId: LibraryIdentifier,
    val userId: Int,
    val oldMd5: String?,
    val failedBeforeZoteroApiRequest: Boolean = true,

    val apiClient: SyncApi,
    val dbWrapper: DbWrapper,
    val fileStore: FileStore,
    val schemaController: SchemaController
): SyncAction<Unit> {

    override suspend fun result() {
        //TODO
    }

    private suspend fun markAttachmentAsUploaded(version: Int?) {
        try {
            var requests: MutableList<DbRequest> = mutableListOf( MarkAttachmentUploadedDbRequest(libraryId = this.libraryId, key = this.key, version = version))
            if (version != null) {
                requests.add(UpdateVersionsDbRequest(version = version, libraryId =this.libraryId, type=  UpdateVersionType.objectS(
                    SyncObject.item)))
            }
            dbWrapper.realmDbStorage.perform(requests)
        }catch (error: Exception) {
            Timber.e("UploadAttachmentSyncAction: can't mark attachment as uploaded - $error")
            throw error
        }

    }

    private suspend fun checkDatabase() {
        try {
            val request = CheckItemIsChangedDbRequest(libraryId = this.libraryId, key = this.key)
            val isChanged = dbWrapper.realmDbStorage.perform(request = request, invalidateRealm = true)
            if (!isChanged) {
                return
            } else {
                Timber.e("UploadAttachmentSyncAction: attachment item not submitted")
                throw SyncActionError.attachmentItemNotSubmitted
            }
        } catch (e: Exception) {
            Timber.e("UploadAttachmentSyncAction: could not check item submitted - $e")
            throw e
        }
    }

    private suspend fun validateFile(): Long {
        val size = this.file.length()

        if (size > 0) {
            if (this.file.extension != "pdf" || fileStore.isPdf(file = this.file)) {
                return size
            }
            this.file.delete()
        }

        Timber.e("UploadAttachmentSyncAction: missing attachment - ${this.file.absolutePath}")
        val item = dbWrapper.realmDbStorage.perform(
            ReadItemDbRequest(
                libraryId = this.libraryId,
                key = this.key
            )
        )
        val title = item.displayTitle
        //TODO invalidate realm
        throw SyncActionError.attachmentMissing(
            key = this.key,
            libraryId = this.libraryId,
            title = title
        )

    }


}