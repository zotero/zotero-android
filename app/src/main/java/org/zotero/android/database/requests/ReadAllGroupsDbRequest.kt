package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RGroup

class ReadAllGroupsDbRequest : DbResponseRequest<RealmResults<RGroup>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RealmResults<RGroup> {
        val results = database.where<RGroup>().notSyncState(ObjectSyncState.dirty).findAll()
        results.sortByDescending { it.orderId }
        results.sortBy { it.name }
        return results
    }
}