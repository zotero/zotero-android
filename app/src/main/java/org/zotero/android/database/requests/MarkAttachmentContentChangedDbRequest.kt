package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier

class MarkAttachmentContentChangedDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val md5: String,
    private val mtime: Long,
) : DbRequest {
    override val needsWrite: Boolean = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(key, libraryId).findFirst() ?: return
        val md5Field = item.fields.where().key(FieldKeys.Item.Attachment.md5).findFirst() ?: return
        val mtimeField = item.fields.where().key(FieldKeys.Item.Attachment.mtime).findFirst() ?: return
        md5Field.value = md5
        mtimeField.value = mtime.toString()
        item.attachmentNeedsSync = true
        item.changeType = UpdatableChangeType.syncResponse.name
    }
}
