package org.zotero.android.architecture

import android.net.Uri
import org.zotero.android.sync.AttachmentFileDeletedNotification
import java.io.File

object EventBusConstants {
    data class FileWasSelected(
        val uri: Uri?,
        val callPoint: CallPoint
    ) {
        enum class CallPoint{
            AllItems, ItemDetails
        }
    }

    data class AttachmentDeleted(val file: File)
    data class AttachmentFileDeleted(val notification: AttachmentFileDeletedNotification)
    data class OnKeyboardVisibilityChange(val isOpen: Boolean)
}