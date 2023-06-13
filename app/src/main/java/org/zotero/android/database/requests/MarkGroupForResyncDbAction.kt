package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RGroup

class MarkGroupForResyncDbAction(
    val identifier: Int
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val existing = database.where<RGroup>().equalTo("identifier", this.identifier).findFirst()
        if (existing != null) {
            if (existing.syncState == ObjectSyncState.synced.name) {
                existing.syncState = ObjectSyncState.outdated.name
            }
        } else {
            val library = database.createObject<RGroup>()
            library.identifier = this.identifier
            library.syncState = ObjectSyncState.dirty.name
        }
    }
}