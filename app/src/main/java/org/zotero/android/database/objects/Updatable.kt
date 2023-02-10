package org.zotero.android.database.objects

import io.realm.Realm
import io.realm.RealmList

interface Updatable {
    var changes: RealmList<RObjectChange>

    var changeType: String //UpdatableChangeType
    val updateParameters: Map<String, Any>?
    val selfOrChildChanged: Boolean

    fun markAsChanged(database: Realm)

    fun deleteChanges(uuids: List<String>, database: Realm) {
        if (this.isChanged && !uuids.isEmpty()) {
            this.changes.filter { uuids.contains(it.identifier) }.forEach {
                it.deleteFromRealm()
            }
            this.changeType = UpdatableChangeType.syncResponse.name
        }
    }

    fun deleteAllChanges(database: Realm) {
        changes.deleteAllFromRealm()
    }

    val isChanged: Boolean
        get() {
            return changes.isNotEmpty()
        }


}