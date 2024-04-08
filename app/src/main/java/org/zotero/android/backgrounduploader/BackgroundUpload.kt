package org.zotero.android.backgrounduploader

import org.zotero.android.sync.LibraryIdentifier
import java.io.File
import java.util.Date

data class BackgroundUpload(
    val type: Kind,
    val key: String,
    val libraryId: LibraryIdentifier,
    val userId: Long,
    val remoteUrl: String,
    val fileUrl: File,
    val md5: String,
    val sessionId: String = "",
    val date: Date,
    val size: Long = 0L,
) {
    sealed class Kind {
        data class zotero(val uploadKey: String): Kind()
        data class webdav(val mtime: Long): Kind()
    }

}