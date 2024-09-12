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

class EditAnnotationRotationDbRequest(
    val key: String,
    val libraryId: LibraryIdentifier,
    val rotation: Int,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.key, this.libraryId).findFirst() ?: return

        val field: RItemField
        val _field =
            item.fields.where().key(FieldKeys.Item.Annotation.Position.rotation).findFirst()
        if (_field != null) {
            field = _field
        } else {
            field = database.createEmbeddedObject(RItemField::class.java, item, "fields")
            field.key = FieldKeys.Item.Annotation.Position.rotation
            field.baseKey = FieldKeys.Item.Annotation.position
        }

        field.value = "${this.rotation}"
        item.changeType = UpdatableChangeType.user.name
        item.changes.add(RObjectChange.create(listOf(RItemChanges.fields)))
    }
}