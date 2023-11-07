package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier
import java.util.Date

class EndItemDetailEditingDbRequest(
    private val libraryId: LibraryIdentifier,
    private val itemKey: String
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(itemKey, libraryId).findFirst() ?: return
        item.dateModified = Date()
        item.changesSyncPaused = false
        item.changeType = UpdatableChangeType.user.name
    }

}