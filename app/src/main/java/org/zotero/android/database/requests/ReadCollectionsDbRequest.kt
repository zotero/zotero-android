package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RCollection
import org.zotero.android.sync.LibraryIdentifier

class ReadCollectionsDbRequest(
    val libraryId: LibraryIdentifier,
    val excludedKeys: Set<String> = emptySet()
): DbResponseRequest<RealmResults<RCollection>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RealmResults<RCollection> {
        return database.where<RCollection>()
            .notSyncState(ObjectSyncState.dirty, this.libraryId)
            .and()
            .deleted(false)
            .and()
            .keyNotIn(this.excludedKeys)
            .findAll()
    }
}