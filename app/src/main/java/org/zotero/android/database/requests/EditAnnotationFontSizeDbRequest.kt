package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier

class EditAnnotationFontSizeDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val size: Int
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.key, this.libraryId).findFirst() ?: return

        val field: RItemField
        val _field = item.fields.where().key(FieldKeys.Item.Annotation.Position.fontSize).findFirst()
        if (_field != null) {
            field = _field
        } else {
            field = database.createEmbeddedObject(RItemField::class.java, item, "fields")
            field.key = FieldKeys.Item.Annotation.Position.fontSize
            field.baseKey = FieldKeys.Item.Annotation.position
        }

        field.value = "${this.size}"
        item.changeType = UpdatableChangeType.user.name
        item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.fields)))
    }
}