package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RGroup

class ReadAllWritableGroupsDbRequest : DbResponseRequest<RealmResults<RGroup>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RealmResults<RGroup> {
        return database.where<RGroup>()
            .notSyncState(ObjectSyncState.dirty)
            .and()
            .rawPredicate("canEditMetadata == true")
            .sort("orderId", Sort.DESCENDING, "name", Sort.ASCENDING)
            .findAll()
    }
}