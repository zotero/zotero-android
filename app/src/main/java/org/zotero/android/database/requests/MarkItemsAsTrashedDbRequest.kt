package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier

class MarkItemsAsTrashedDbRequest(
    val keys: List<String>,
    val libraryId: LibraryIdentifier,
    val trashed: Boolean,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val items = database.where<RItem>().keys(this.keys, this.libraryId).findAll()
        items.forEach { item ->
            item.trash = this.trashed
            item.changeType = UpdatableChangeType.user.name
            item.changes.add(RObjectChange.create(listOf(RItemChanges.trash)))
        }
    }
}