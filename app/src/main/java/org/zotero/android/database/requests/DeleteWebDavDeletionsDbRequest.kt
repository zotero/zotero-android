package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RWebDavDeletion
import org.zotero.android.sync.LibraryIdentifier

class DeleteWebDavDeletionsDbRequest(
    private val keys: Set<String>,
    private val libraryId: LibraryIdentifier,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        database
            .where<RWebDavDeletion>()
            .keys(this.keys, this.libraryId)
            .findAll()
            .deleteAllFromRealm()
    }
}