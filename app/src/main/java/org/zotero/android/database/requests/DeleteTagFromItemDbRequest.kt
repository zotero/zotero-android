package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.RTag
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier

class DeleteTagFromItemDbRequest(
    val key: String,
    val libraryId: LibraryIdentifier,
    val tagName: String,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.key, this.libraryId).findFirst()
        if (item == null) {
            return
        }
        val tagsToRemove = item.tags?.where()?.tagName(this.tagName)?.findAll()

        if (tagsToRemove == null || tagsToRemove.isEmpty()) {
            return
        }

        val baseTagsToRemove =
            ReadBaseTagsToDeleteDbRequest(fromTags = tagsToRemove).process(database)
        tagsToRemove.deleteAllFromRealm()

        if (!baseTagsToRemove.isEmpty()) {
            database.where<RTag>().nameIn(baseTagsToRemove).findAll().deleteAllFromRealm()
        }
        item.rawType = item.rawType
        item.changeType = UpdatableChangeType.user.name
        item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.tags)))
    }
}