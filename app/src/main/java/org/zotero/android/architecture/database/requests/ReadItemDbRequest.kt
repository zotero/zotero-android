package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.architecture.database.DbError
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.sync.LibraryIdentifier

class ReadItemDbRequest(
    val libraryId: LibraryIdentifier,
    val key: String,
) : DbResponseRequest<RItem> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RItem {
        return database
            .where<RItem>()
            .key(key, libraryId)
            .findFirst() ?: throw DbError.objectNotFound
    }

}