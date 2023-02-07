package org.zotero.android.architecture.attachmentdownloader

import org.zotero.android.api.SyncApi
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.files.FileStore
import org.zotero.android.sync.LibraryIdentifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentDownloader @Inject constructor(
    private val observable: DownloaderEventStream,
    private val apiClient: SyncApi,
    private val fileStorage: FileStore,
    private val dbWrapper: DbWrapper,
) {
    sealed class Error : Exception() {
        object incompatibleAttachment : Error()
        object zipDidntContainRequestedFile : Error()
        object cantUnzipSnapshot: Error()
    }

    data class Download(
        val key: String,
        val libraryId: LibraryIdentifier,
    )

    data class Update(
        val key: String,
        val parentKey: String?,
        val libraryId: LibraryIdentifier,
        val kind: Kind,
    ) {
        sealed class Kind {
            data class progress(val progressInHundreds: Int) : Kind()
            object ready : Kind()
            data class failed(val exception: Exception) : Kind()
            object cancelled : Kind()
        }


        companion object {
            fun init(
                key: String,
                parentKey: String?,
                libraryId: LibraryIdentifier,
                kind: Kind
            ): Update {
                return Update(
                    key = key,
                    parentKey = parentKey,
                    libraryId = libraryId,
                    kind = kind,
                )

            }

            fun init(download: Download, parentKey: String?, kind: Kind): Update {
                return Update(
                    key = download.key,
                    parentKey = parentKey,
                    libraryId = download.libraryId,
                    kind = kind,
                )
            }
        }
    }

    private var userId: Long = 0L

    fun init(userId: Long) {
        this.userId = userId
    }


}