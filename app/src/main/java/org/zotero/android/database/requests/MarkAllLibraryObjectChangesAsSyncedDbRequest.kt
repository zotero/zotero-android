package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.Deletable
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RSearch
import org.zotero.android.sync.LibraryIdentifier

class MarkAllLibraryObjectChangesAsSyncedDbRequest constructor(
    val libraryId: LibraryIdentifier
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        deleteObjects(
            database
                .where<RItem>()
                .deleted(true, this.libraryId), database
        )
        deleteObjects(
            database
                .where<RCollection>()
                .deleted(true, this.libraryId), database
        )
        deleteObjects(
            database
                .where<RSearch>()
                .deleted(true, this.libraryId), database)

        for (objectS in database
            .where<RCollection>()
            .changesWithoutDeletions(this.libraryId)
            .findAll()) {
            objectS.deleteAllChanges(database = database)
        }
        for (objectS in database
            .where<RItem>()
            .changesWithoutDeletions(this.libraryId).findAll()) {
            objectS.deleteAllChanges(database = database)
        }
        for (objectS in database
            .where<RSearch>()
            .changesWithoutDeletions(this.libraryId)
            .findAll()) {
            objectS.deleteAllChanges(database = database)
        }

    }

    private fun <T : Deletable> deleteObjects(query: RealmQuery<T>, database: Realm) {
        val objects = query.findAll()
        for (objectS in objects) {
            if (objectS.isInvalidated) {
                continue
            }
            objectS.willRemove(database)
        }
        objects.deleteAllFromRealm()
    }
}