package org.zotero.android.sync.syncactions

import org.zotero.android.database.requests.ReadAttachmentUploadsDbRequest
import org.zotero.android.sync.AttachmentUpload
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.syncactions.architecture.SyncAction


class LoadUploadDataSyncAction constructor(
    val libraryId: LibraryIdentifier
) : SyncAction() {
    fun result(): List<AttachmentUpload> {
        val uploads = loadUploads(libraryId = this.libraryId)
        val backgroundUploads = this.backgroundUploaderContext.uploads.map { it.md5 }
        return uploads.filter { !backgroundUploads.contains(it.md5) }
    }

    private fun loadUploads(libraryId: LibraryIdentifier): List<AttachmentUpload> {
        val request = ReadAttachmentUploadsDbRequest(libraryId = libraryId, fileStorage = fileStore)
        return dbWrapper.realmDbStorage.perform(request = request, invalidateRealm = true)
    }
}