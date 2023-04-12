package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbError
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier

class MarkCollectionAndItemsAsDeletedDbRequest(
    val key: String,
    val libraryId: LibraryIdentifier,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val objectS = database.where<RCollection>().key(this.key, this.libraryId).findFirst() ?: throw DbError.objectNotFound
        objectS.items.forEach {
            it.deleted = true
            it.changeType = UpdatableChangeType.user.name
        }
        objectS.deleted = true
        objectS.changeType = UpdatableChangeType.user.name
    }
}