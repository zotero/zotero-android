package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.architecture.database.DbError
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.RCollection
import org.zotero.android.sync.LibraryIdentifier
import kotlin.reflect.KClass

class ReadCollectionDbRequest(
    val libraryId: LibraryIdentifier,
    val key: String
) : DbResponseRequest<RCollection, RCollection> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm, clazz: KClass<RCollection>?): RCollection {
        return database
            .where<RCollection>()
            .key(key, libraryId)
            .findFirst() ?: throw DbError.objectNotFound
    }
}