package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier

class RemoveItemFromParentDbRequest(
    val key: String,
    val libraryId: LibraryIdentifier,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.key, this.libraryId).findFirst() ?: return
        if (item.parent == null) return
        item.parent?.changeType = UpdatableChangeType.user.name
        item.parent = null
        item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.parent)))
        item.changeType = UpdatableChangeType.user.name
    }
}