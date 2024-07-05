package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbError
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.ktx.uniqueObject
import org.zotero.android.sync.LibraryIdentifier

class StoreMtimeForAttachmentDbRequest(
    private val mtime: Long,
    private val key: String,
    private val libraryId: LibraryIdentifier,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {

        val item = database.where<RItem>().findAll().uniqueObject(key = key, libraryId = libraryId)
        val field = item?.fields?.where()?.key(FieldKeys.Item.Attachment.mtime)?.findFirst()
        if (field == null) {
            throw DbError.objectNotFound
        }

        field.value = "${this.mtime}"
        field.changed = true
        item.changes.add(RObjectChange.create(listOf(RItemChanges.fields)))
        item.changeType = UpdatableChangeType.user.name
    }
}