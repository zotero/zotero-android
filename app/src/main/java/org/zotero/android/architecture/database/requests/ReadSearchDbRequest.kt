package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.architecture.database.DbError
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.RSearch
import org.zotero.android.sync.LibraryIdentifier
import kotlin.reflect.KClass

class ReadSearchDbRequest(
    val libraryId: LibraryIdentifier,
    val key: String,
): DbResponseRequest<RSearch, RSearch> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm, clazz: KClass<RSearch>?): RSearch {
        val search = database.where<RSearch>().key(this.key, this.libraryId).findFirst()
        if (search == null) {
            throw DbError.objectNotFound
        }
        return search
    }
}