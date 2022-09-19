package org.zotero.android.architecture.database.objects

import io.realm.Realm
import io.realm.RealmList

interface Updatable {
    var rawChangedFields: RealmList<String>

    var changeType: String //UpdatableChangeType
    val updateParameters: Map<String, Any>?
    val selfOrChildChanged: Boolean

    fun markAsChanged(database: Realm)

    fun resetChanges() {
        if (!isChanged) {
            return
        }
        rawChangedFields = RealmList()
        changeType = UpdatableChangeType.sync.name
    }

    val isChanged: Boolean
        get() {
            return rawChangedFields.isNotEmpty()
        }


}