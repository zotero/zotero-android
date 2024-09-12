package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.RTag
import org.zotero.android.database.objects.RTypedTag
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag
import java.util.Date

class EditTagsForItemDbRequest(
    val key: String,
    val libraryId: LibraryIdentifier,
    val tags: List<Tag>,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.key, this.libraryId).findFirst() ?: return

        var tagsDidChange = false

        val tagsToRemove = item.tags!!.where().tagNameNotIn(this.tags.map { it.name }).findAll()
        if (!tagsToRemove.isEmpty()) {
            tagsDidChange = true
        }
        val baseTagsToRemove =
            ReadBaseTagsToDeleteDbRequest(fromTags = tagsToRemove).process(database)

        tagsToRemove.deleteAllFromRealm()
        if (!baseTagsToRemove.isEmpty()) {
            database.where<RTag>().nameIn(baseTagsToRemove).findAll().deleteAllFromRealm()
        }

        for (tag in this.tags) {
            if (item.tags.where().tagName(tag.name).findFirst() != null) {
                continue
            }

            val rTag: RTag

            val existing = database.where<RTag>().name(tag.name, this.libraryId).findFirst()
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
            item.changeType = UpdatableChangeType.user.name
            item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.tags)))
            item.dateModified = Date()
        }
    }
}