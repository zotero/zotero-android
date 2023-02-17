package org.zotero.android.architecture

import android.net.Uri
import org.zotero.android.sync.AttachmentFileDeletedNotification
import java.io.File

object EventBusConstants {
    data class FileWasSelected(
        val uri: Uri?
    )

    data class AttachmentDeleted(val file: File)
    data class AttachmentFileDeleted(val notification: AttachmentFileDeletedNotification)
}