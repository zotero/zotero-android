package org.zotero.android.screens.addbyidentifier.data

import org.zotero.android.attachmentdownloader.RemoteAttachmentDownloader
import org.zotero.android.database.objects.Attachment
import org.zotero.android.sync.LibraryIdentifier

data class LookupRowItem(
    val identifier: String,
    val key: String,
    val type: String,
    val title: String
)

sealed interface LookupRow {

    enum class IdentifierState {
        enqueued,
        inProgress,
        failed,
    }

    data class identifier(val identifier: String, val state: IdentifierState): LookupRow
    data class item(val item: LookupRowItem): LookupRow
    data class attachment(val attachment: Attachment, val updateKind: RemoteAttachmentDownloader.Update.Kind): LookupRow

    fun isAttachment(key: String, libraryId: LibraryIdentifier): Boolean {
        when (this) {
            is attachment -> {
                return this.attachment.key == key && this.attachment.libraryId == libraryId
            }

            is item, is identifier -> {
                return false
            }
        }
    }
}