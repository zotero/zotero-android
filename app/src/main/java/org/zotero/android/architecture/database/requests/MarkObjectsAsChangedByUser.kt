package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.architecture.database.DbRequest
import org.zotero.android.architecture.database.objects.ObjectSyncState
import org.zotero.android.architecture.database.objects.RCollection
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.RSearch
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncObject
import timber.log.Timber

class MarkObjectsAsChangedByUser(
    val libraryId: LibraryIdentifier,
    val collections: List<String>,
    val items: List<String>
) : DbRequest {

    override val needsWrite: Boolean
        get() {
            return true
        }

    override fun process(database: Realm) {
        markCollections(this.collections, database)
        markItems(this.items, database)
    }

    private fun markItems(keys: List<String>, database: Realm) {
        val objects = database
            .where<RItem>()
            .keys(keys, this.libraryId)
            .findAll()
        for (o in objects) {
            if (o.isInvalidated) {
                continue
            }
            o.markAsChanged(database)
        }
    }

    private fun markCollections(keys: List<String>, database: Realm) {
        val objects = database
            .where<RCollection>()
            .keys(keys, this.libraryId)
            .findAll()
        for (objectS in objects) {
            if (objectS.isInvalidated) {
                continue
            }
            objectS.markAsChanged(database)
        }
    }
}

class MarkOtherObjectsAsChangedByUser(
    val syncObject: SyncObject,
    val versions: Map<String, Int>,
    val libraryId: LibraryIdentifier
) : DbRequest {

    override val needsWrite: Boolean
        get() {
            return true
        }


    override fun process(database: Realm) {
        when (this.syncObject) {
            SyncObject.collection -> {
                val objects = database
                    .where<RCollection>()
                    .library(this.libraryId)
                    .syncState(ObjectSyncState.synced)
                    .findAll()
                markAsChangedCollection(
                    notIn = this.versions,
                    objects = objects,
                    database = database
                )
            }
            SyncObject.search -> {
                val objects = database
                    .where<RSearch>()
                    .library(this.libraryId)
                    .syncState(ObjectSyncState.synced)
                    .findAll()
                markAsChangedSearch(
                    notIn = this.versions,
                    objects = objects,
                    database = database
                )
            }

            SyncObject.item -> {
                val objects = database
                    .where<RItem>()
                    .library(this.libraryId)
                    .syncState(ObjectSyncState.synced)
                    .isTrash(false)
                    .findAll()
                markAsChangedItem(
                    notIn = this.versions,
                    objects = objects,
                    database = database
                )
            }

            SyncObject.trash -> {
                val objects = database
                    .where<RItem>()
                    .library(this.libraryId)
                    .syncState(ObjectSyncState.synced)
                    .isTrash(true)
                    .findAll()
                markAsChangedItem(
                    notIn = this.versions,
                    objects = objects,
                    database = database
                )
            }
            SyncObject.settings -> {}
        }
    }

    private fun markAsChangedCollection(
        notIn: Map<String, Int>,
        objects: RealmResults<RCollection>,
        database: Realm
    ) {
        for (objectS in objects) {
            if (objectS.isInvalidated || this.versions.keys.contains(objectS.key)) {
                continue
            }
            if (objectS.deleted) {
                Timber.w("MarkOtherObjectsAsChangedByUser: full sync locally " +
                        "deleted missing remotely ${objectS.key}")
                objectS.deleteFromRealm()
            } else {
                Timber.w("MarkOtherObjectsAsChangedByUser: full sync " +
                        "marked ${objectS.key} as changed")
                objectS.markAsChanged(database)
            }
        }
    }

    private fun markAsChangedSearch(
        notIn: Map<String, Int>,
        objects: RealmResults<RSearch>,
        database: Realm
    ) {
        for (objectS in objects) {
            if (objectS.isInvalidated || this.versions.keys.contains(objectS.key)) {
                continue
            }
            if (objectS.deleted) {
                Timber.w("MarkOtherObjectsAsChangedByUser: full sync locally" +
                        " deleted missing remotely ${objectS.key}")
                objectS.deleteFromRealm()
            } else {
                Timber.w("MarkOtherObjectsAsChangedByUser: full sync " +
                        "marked ${objectS.key} as changed")
                objectS.markAsChanged(database)
            }
        }
    }

    private fun markAsChangedItem(
        notIn: Map<String, Int>,
        objects: RealmResults<RItem>,
        database: Realm
    ) {
        for (objectS in objects) {
            if (objectS.isInvalidated || this.versions.keys.contains(objectS.key)) {
                continue
            }
            if (objectS.deleted) {
                Timber.w("MarkOtherObjectsAsChangedByUser: full sync locally " +
                        "deleted missing remotely ${objectS.key}")
                objectS.deleteFromRealm()
            } else {
                Timber.w("MarkOtherObjectsAsChangedByUser: full sync " +
                        "marked ${objectS.key} as changed")
                objectS.markAsChanged(database)
            }
        }
    }
}
