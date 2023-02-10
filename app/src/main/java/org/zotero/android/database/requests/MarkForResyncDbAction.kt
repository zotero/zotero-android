package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmModel
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.Syncable
import org.zotero.android.database.objects.Updatable
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier
import java.util.Date
import kotlin.reflect.KClass

class MarkForResyncDbAction(
    val libraryId: LibraryIdentifier,
    val keys: List<String>,
    var clazz: KClass<out RealmModel>,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val syncDate = Date()
        var toCreate = this.keys.toMutableList()
        val objects = database.where(clazz.java).keys(this.keys, this.libraryId).findAll()
        for (objectS in objects) {
            objectS as Syncable
            objectS as Updatable
            if (objectS.syncState == ObjectSyncState.synced.name) {
                objectS.syncState = ObjectSyncState.outdated.name
            }
            objectS.syncRetries += 1
            objectS.lastSyncDate = syncDate
            objectS.changeType = UpdatableChangeType.syncResponse.name
            val index = toCreate.indexOf(objectS.key)
            if (index != -1)
                toCreate.removeAt(index)
        }
        for (key in toCreate) {
            val objectS = database.createObject(clazz.java)
            objectS as Syncable
            objectS as Updatable
            objectS.key = key
            objectS.syncState = ObjectSyncState.dirty.name
            objectS.syncRetries = 1
            objectS.lastSyncDate = syncDate
            objectS.libraryId = this.libraryId
        }
    }
}