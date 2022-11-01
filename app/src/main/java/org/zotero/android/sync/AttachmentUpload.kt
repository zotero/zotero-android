package org.zotero.android.sync

import java.io.File

data class AttachmentUpload(
    val libraryId: LibraryIdentifier,
    val key: String,
    val filename: String,
    val contentType: String,
    val md5: String,
    val mtime: Int,
    val file: File,
    val oldMd5: String?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttachmentUpload

        if (libraryId != other.libraryId) return false
        if (key != other.key) return false
        if (filename != other.filename) return false
        if (contentType != other.contentType) return false
        if (md5 != other.md5) return false
        if (mtime != other.mtime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = libraryId.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + md5.hashCode()
        result = 31 * result + mtime
        return result
    }
}