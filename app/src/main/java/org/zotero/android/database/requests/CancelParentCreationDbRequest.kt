package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.sync.LibraryIdentifier

class CancelParentCreationDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.key, this.libraryId).findFirst() ?: return

        if (item.parent == null) {
            return
        }
        item.parent = null
        val parentChange = item.changes.filter { change ->
            change.rawChanges.contains(RItemChanges.parent.name)
        }
        item.changesSyncPaused = false

        parentChange.forEach {
            it.deleteFromRealm()
        }
    }
}