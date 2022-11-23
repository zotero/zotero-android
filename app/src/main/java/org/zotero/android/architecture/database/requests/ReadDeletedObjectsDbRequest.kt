package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmResults
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.sync.LibraryIdentifier
import kotlin.reflect.KClass

class ReadDeletedObjectsDbRequest<T : RealmObject>(val libraryId: LibraryIdentifier) :
    DbResponseRequest<T, RealmResults<T>> {

    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm, clazz: KClass<T>?): RealmResults<T> {
        return database
            .where(clazz!!.java)
            .deleted(true, libraryId)
            .findAll()
    }

}
