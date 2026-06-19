package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.backgrounduploader.BackgroundUploaderContext
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.ReadAttachmentUploadsDbRequest
import org.zotero.android.sync.AttachmentUpload
import org.zotero.android.sync.LibraryIdentifier


class LoadUploadDataSyncAction @AssistedInject constructor(
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,

    private val backgroundUploaderContext: BackgroundUploaderContext,
    private val readAttachmentUploadsDbRequestFactory: ReadAttachmentUploadsDbRequest.Factory,
    private val dbWrapperMain: DbWrapperMain,
) {
    fun result(): List<AttachmentUpload> {
        val uploads = loadUploads(libraryId = this.libraryId)
        val backgroundUploads = this.backgroundUploaderContext.uploads.map { it.md5 }
        return uploads.filter { !backgroundUploads.contains(it.md5) }
    }

    private fun loadUploads(libraryId: LibraryIdentifier): List<AttachmentUpload> {
        val request = readAttachmentUploadsDbRequestFactory.create(
            libraryId = libraryId,
        )
        return dbWrapperMain.realmDbStorage.perform(request = request, invalidateRealm = true)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("libraryId") libraryId: LibraryIdentifier
        ): LoadUploadDataSyncAction
    }

}