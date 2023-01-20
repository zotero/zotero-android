package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.RealmModel
import org.zotero.android.architecture.database.DbRequest
import org.zotero.android.architecture.database.objects.Deletable
import org.zotero.android.architecture.database.objects.Updatable
import org.zotero.android.architecture.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier
import kotlin.reflect.KClass

class MarkObjectsAsDeletedDbRequest(
    var clazz: KClass<out RealmModel>,
    val keys: List<String>,
    val libraryId: LibraryIdentifier
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val objects = database.where(clazz.java).keys(this.keys, this.libraryId).findAll()
        for (objectS in objects) {
            objectS as Deletable
            objectS as Updatable
            if (objectS.deleted) {
                continue
            }
            objectS.deleted = true
            objectS.changeType = UpdatableChangeType.user.name
        }
    }
}