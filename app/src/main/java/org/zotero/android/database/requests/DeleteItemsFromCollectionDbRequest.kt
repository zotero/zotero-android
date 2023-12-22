package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier

class DeleteItemsFromCollectionDbRequest(
    private val collectionKey: String,
    private val itemKeys: Set<String>,
    private val libraryId: LibraryIdentifier,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val collection =
            database.where<RCollection>().key(this.collectionKey, this.libraryId).findFirst()
                ?: return

        val items = database.where<RItem>().keys(this.itemKeys, this.libraryId).findAll()
        for (item in items) {
            val index = collection.items.indexOf(item)
            if (index != -1) {
                collection.items.removeAt(index)
                item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.collections)))
                item.changeType = UpdatableChangeType.user.name
            }
        }
    }
}