package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbError
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.sync.LibraryIdentifier

class ReadCollectionDbRequest(
    val libraryId: LibraryIdentifier,
    val key: String
) : DbResponseRequest<RCollection> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RCollection {
        return database
            .where<RCollection>()
            .key(key, libraryId)
            .findFirst() ?: throw DbError.objectNotFound
    }
}