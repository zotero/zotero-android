package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RCollectionChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier

class EditCollectionDbRequest(
    val libraryId: LibraryIdentifier,
    val key: String,
    val name: String,
    val parentKey: String?,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val  collection = database.where<RCollection>().key(this.key, this.libraryId).findFirst() ?: return

        val changes = mutableListOf<RCollectionChanges>()

        if (collection.name != this.name) {
            collection.name = this.name
            changes.add(RCollectionChanges.nameS)
        }

        if (collection.parentKey != this.parentKey) {
            collection.parentKey = this.parentKey
            changes.add(RCollectionChanges.parent)
        }

        collection.changes.add(RObjectChange.create(changes = changes))
        collection.changeType = UpdatableChangeType.user.name
    }
}