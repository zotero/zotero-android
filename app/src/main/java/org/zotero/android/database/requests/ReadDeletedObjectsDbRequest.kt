package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmResults
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.sync.LibraryIdentifier
import kotlin.reflect.KClass

class ReadDeletedObjectsDbRequest<T : RealmObject>(
    val libraryId: LibraryIdentifier,
    val clazz: KClass<T>) :
    DbResponseRequest<RealmResults<T>> {

    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RealmResults<T> {
        return database
            .where(clazz.java)
            .deleted(true, libraryId)
            .findAll()
    }

}
