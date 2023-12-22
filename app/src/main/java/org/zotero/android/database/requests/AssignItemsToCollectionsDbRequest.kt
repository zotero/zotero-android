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

class AssignItemsToCollectionsDbRequest(
    private val collectionKeys: Set<String>,
    private val itemKeys: Set<String>,
    private val libraryId: LibraryIdentifier,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val collections =
            database.where<RCollection>().keys(this.collectionKeys, this.libraryId).findAll()
        val items = database.where<RItem>().keys(this.itemKeys, this.libraryId).findAll()
        for (collection in collections) {
            for (item in items) {
                if (collection.items.where().key(item.key).findFirst() != null) {
                    continue
                }
                collection.items.add(item)
                item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.collections)))
                item.changeType = UpdatableChangeType.user.name
            }
        }
    }
}