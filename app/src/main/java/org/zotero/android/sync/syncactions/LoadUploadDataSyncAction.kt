package org.zotero.android.sync.syncactions

import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.requests.ReadAttachmentUploadsDbRequest
import org.zotero.android.backgrounduploader.BackgroundUploaderContext
import org.zotero.android.files.FileStore
import org.zotero.android.sync.AttachmentUpload
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncAction

class LoadUploadDataSyncAction constructor(
    val backgroundUploaderContext: BackgroundUploaderContext,
    val dbWrapper: DbWrapper,
    val fileStore: FileStore,
    val libraryId: LibraryIdentifier
): SyncAction<List<AttachmentUpload>> {
    override suspend fun result(): List<AttachmentUpload> {
        val uploads = loadUploads(libraryId = this.libraryId)
        val backgroundUploads = this.backgroundUploaderContext.uploads.map { it.md5 }
        return uploads.filter { !backgroundUploads.contains(it.md5) }
    }

    private fun loadUploads(libraryId: LibraryIdentifier): List<AttachmentUpload> {
        val request = ReadAttachmentUploadsDbRequest(libraryId = libraryId, fileStorage = fileStore)
        return dbWrapper.realmDbStorage.perform(request = request, invalidateRealm = true)
    }
}