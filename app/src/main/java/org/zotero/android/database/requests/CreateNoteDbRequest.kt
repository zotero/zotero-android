package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.RTag
import org.zotero.android.database.objects.RTypedTag
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Note
import timber.log.Timber
import java.util.Date

class CreateNoteDbRequest(
    val note: Note,
    val localizedType: String,
    val libraryId: LibraryIdentifier,
    val collectionKey: String?,
    val parentKey: String?,
): DbResponseRequest<RItem> {
    sealed class Error : Exception() {
        object alreadyExists : Error()
    }

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): RItem {

        if(database.where<RItem>().key(this.note.key, this.libraryId).findFirst() != null) {
            Timber.e("CreateNoteDbRequest: Trying to create note that already exists!")
            throw Error.alreadyExists
        }

        val changes = mutableListOf<RItemChanges>(RItemChanges.type, RItemChanges.fields)

        val item = database.createObject<RItem>()
        item.key = this.note.key
        item.rawType = ItemTypes.note
        item.localizedType = this.localizedType
        item.syncState = ObjectSyncState.synced.name
        item.set(title = this.note.title)
        item.changeType = UpdatableChangeType.user.name
        item.dateAdded = Date()
        item.dateModified = Date()
        item.libraryId = libraryId

        val itemKey = this.parentKey
        if (itemKey != null) {
            val parent = database.where<RItem>().key(itemKey, this.libraryId).findFirst()
            if (parent != null) {
                item.parent = parent
                changes.add(RItemChanges.parent)
                parent.version = parent.version
            }

        }

        val collectionKey = this.collectionKey
        if (collectionKey != null) {
            val collection = database.where<RCollection>().key(collectionKey, this.libraryId).findFirst()
            if (collection != null) {
                collection.items.add(item)
                changes.add(RItemChanges.collections)
            }
        }

        val noteField = database.createEmbeddedObject(RItemField::class.java, item, "fields")
        noteField.key = FieldKeys.Item.note
        noteField.baseKey = null
        noteField.value = this.note.text
        noteField.changed = true

        for (tag in this.note.tags) {
            val rTag: RTag
            val existing = database.where<RTag>().library(this.libraryId).name(tag.name).findFirst()
            if (existing != null) {
                rTag = existing
            } else {
                rTag = database.createObject<RTag>()
                rTag.name = tag.name
                rTag.updateSortName()
                rTag.color = tag.color
                rTag.libraryId = this.libraryId
            }

            val rTypedTag = database.createObject<RTypedTag>()
            rTypedTag.type = RTypedTag.Kind.manual.name
            rTypedTag.item = item
            rTypedTag.tag = rTag
        }

        item.changes.add(RObjectChange.create(changes = changes))
        return item
    }
}