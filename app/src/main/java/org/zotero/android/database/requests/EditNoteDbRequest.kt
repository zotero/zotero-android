package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbError
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.RTag
import org.zotero.android.database.objects.RTypedTag
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Note
import org.zotero.android.sync.Tag
import java.util.Date

class EditNoteDbRequest(
    val note: Note,
    val libraryId: LibraryIdentifier,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.note.key, this.libraryId).findFirst()
        if (item == null) {
            throw DbError.objectNotFound
        }

        val changes = mutableListOf<RItemChanges>()

        val field = item.fields.where().key(FieldKeys.Item.note).findFirst()

        if (field != null && field.value != this.note.text) {
            item.set(title = this.note.title)
            changes.add(RItemChanges.fields)

            field.value = this.note.text
            field.changed = true
        }

        updateTags(this.note.tags, item = item, changes = changes, database = database)

        if (!changes.isEmpty()) {
            item.changes.add(RObjectChange.create(changes = changes))
            item.changeType = UpdatableChangeType.user.name
            item.dateModified = Date()
        }
    }

    private fun updateTags(tags: List<Tag>, item: RItem, changes: MutableList<RItemChanges>, database: Realm) {
        val tagsToRemove = item.tags!!.where().tagNameNotIn(tags.map({ it.name })).findAll()
        var tagsDidChange = !tagsToRemove.isEmpty()

        tagsToRemove.deleteAllFromRealm()

        for (tag in tags) {
            if (item.tags.where().tagName(tag.name).findFirst() != null) {
                continue
            }

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
            tagsDidChange = true
        }

        if (tagsDidChange) {
            item.rawType = item.rawType
            changes.add(RItemChanges.tags)
        }
    }
}