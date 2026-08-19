package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RItem
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber

/**
 * Deletes placeholder items for keys that the server has confirmed don't exist in the given
 * library. Only items in the `dirty` sync state (placeholders created before any real data was
 * stored for them) with no local changes and no children are deleted; anything else is left for
 * the regular resync/conflict machinery. Returns the keys that were deleted.
 */
class DeleteMissingPlaceholderItemsDbRequest(
    private val libraryId: LibraryIdentifier,
    private val keys: List<String>,
) : DbResponseRequest<List<String>> {

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): List<String> {
        val deletedKeys = mutableListOf<String>()
        val objects = database
            .where<RItem>()
            .keys(this.keys, this.libraryId)
            .syncState(ObjectSyncState.dirty)
            .findAll()
        for (objectS in objects) {
            if (objectS.isInvalidated || objectS.isChanged || !objectS.children!!.isEmpty()) {
                continue
            }
            Timber.w("DeleteMissingPlaceholderItemsDbRequest: deleting placeholder ${objectS.key} missing remotely in $libraryId")
            deletedKeys.add(objectS.key)
            objectS.willRemove(database)
            objectS.deleteFromRealm()
        }
        return deletedKeys
    }
}
