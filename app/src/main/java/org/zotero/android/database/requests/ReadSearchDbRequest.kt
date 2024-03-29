package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbError
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RSearch
import org.zotero.android.sync.LibraryIdentifier

class ReadSearchDbRequest(
    val libraryId: LibraryIdentifier,
    val key: String,
) : DbResponseRequest<RSearch> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RSearch {
        return database
            .where<RSearch>()
            .key(key, libraryId)
            .findFirst() ?: throw DbError.objectNotFound
    }
}