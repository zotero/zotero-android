package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmModel
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.Deletable
import org.zotero.android.sync.LibraryIdentifier
import kotlin.reflect.KClass

class DeleteObjectsDbRequest<T: RealmModel>(
    private val keys: List<String>,
    private val libraryId: LibraryIdentifier,
    val clazz: KClass<T>,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val objects = database.where(clazz.java).keys(this.keys, this.libraryId).findAll()
        for (objectS in objects) {
            objectS as Deletable
            if (objectS.isInvalidated) {
                continue
            }
            objectS.willRemove(database)
        }
        objects.deleteAllFromRealm()
    }
}