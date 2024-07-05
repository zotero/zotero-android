package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier

class MarkAttachmentsNotUploadedDbRequest(
    private val keys: List<String>,
    private val libraryId: LibraryIdentifier,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val attachments = database.where<RItem>().keys(this.keys, this.libraryId).findAll()
        for (attachment in attachments) {
            if(attachment.isInvalidated) { continue }
            attachment.attachmentNeedsSync = true
            attachment.changeType = UpdatableChangeType.syncResponse.name
        }
    }
}