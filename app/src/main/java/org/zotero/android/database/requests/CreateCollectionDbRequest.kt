package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RCollectionChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier

class CreateCollectionDbRequest(
    val libraryId: LibraryIdentifier,
    val key: String,
    val name: String,
    val parentKey: String?,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val collection = database.createObject<RCollection>()
        collection.key = this.key
        collection.name = this.name
        collection.syncState = ObjectSyncState.synced.name
        collection.libraryId = this.libraryId

        val changes = mutableListOf<RCollectionChanges>(RCollectionChanges.nameS)
        val key = this.parentKey
        if (key != null) {
            collection.parentKey = key
            changes.add(RCollectionChanges.parent)

            val parent = database.where<RCollection>().key(key, this.libraryId).findFirst()
            if (parent != null) {
                parent.collapsed = false
            }
        }

        val change = RObjectChange.create(changes = changes)
        collection.changes.add(change)

        collection.changeType = UpdatableChangeType.user.name
    }
}