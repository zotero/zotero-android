package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.architecture.database.DbError
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.sync.LibraryIdentifier
import kotlin.reflect.KClass

class ReadItemDbRequest(
    val libraryId: LibraryIdentifier,
    val key: String,
) : DbResponseRequest<RItem, RItem> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm, clazz: KClass<RItem>?): RItem {
        return database
            .where<RItem>()
            .key(key, libraryId)
            .findFirst() ?: throw DbError.objectNotFound
    }

}