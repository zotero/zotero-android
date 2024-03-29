package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.sync.LibraryIdentifier
import java.util.Date

class UpdateCollectionLastUsedDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val collection = database.where<RCollection>().key(this.key, this.libraryId).findFirst() ?: return
        collection.lastUsed = Date()
    }
}