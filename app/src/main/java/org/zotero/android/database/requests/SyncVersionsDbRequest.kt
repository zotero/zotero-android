package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RGroup
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RSearch
import org.zotero.android.database.objects.Syncable
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.SyncType
import java.lang.Integer.min
import java.util.Date

private typealias ResultSyncVersionsString = List<String>

class SyncVersionsDbRequest(
    private val versions: Map<String, Int>,
    private val libraryId: LibraryIdentifier,
    private val syncObject: SyncObject,
    val syncType: SyncType,
    val delayIntervals: List<Double>
) :
    DbResponseRequest<ResultSyncVersionsString> {
    override val needsWrite: Boolean
        get() = false

    override fun process(
        database: Realm,
    ): ResultSyncVersionsString {
        when (this.syncObject) {
            SyncObject.collection ->
                return check(
                    versions = this.versions,
                    objects = database
                        .where<RCollection>()
                        .findAll()
                )
            SyncObject.search ->
                return check(
                    versions = this.versions,
                    objects = database
                        .where<RSearch>()
                        .findAll()
                )
            SyncObject.item -> {
                val objects = database
                    .where<RItem>()
                    .isTrash(false)
                    .findAll()
                return check(
                    versions = this.versions,
                    objects = objects
                )
            }
            SyncObject.trash -> {
                val objects = database
                    .where<RItem>()
                    .isTrash(true)
                    .findAll()
                return check(
                    versions = this.versions,
                    objects = objects
                )
            }
            SyncObject.settings ->
                return listOf()
        }
    }

    private fun <Obj : Syncable> check(
        versions: Map<String, Int>,
        objects: RealmResults<Obj>
    ): List<String> {
        val date = Date()
        val toUpdate = this.versions.keys.toTypedArray().toMutableList()

        for (objectS in objects) {
            if (objectS.syncState == ObjectSyncState.synced.name) {
                val version = this.versions[objectS.key]
                if (version != null && version == objectS.version) {
                    val index = toUpdate.indexOfFirst { it == objectS.key }
                    if (index != -1) {
                        toUpdate.removeAt(index)
                    }
                }
                continue
            }

            when (this.syncType) {
                SyncType.ignoreIndividualDelays,
                SyncType.full -> {
                    if (toUpdate.contains(objectS.key)) {
                        continue
                    }
                    toUpdate.add(objectS.key)
                }
                SyncType.collectionsOnly,
                SyncType.normal,
                SyncType.keysOnly -> {
                    val delayIdx = min(objectS.syncRetries, (this.delayIntervals.size - 1))
                    val delay = this.delayIntervals[delayIdx]
                    if (objectS.lastSyncDate != null && date.time - objectS.lastSyncDate!!.time >= delay) {
                        if (toUpdate.contains(objectS.key)) {
                            continue
                        }
                        toUpdate.add(objectS.key)
                    } else {
                        val index = toUpdate.indexOfFirst { it == objectS.key }
                        if (index != -1) {
                            toUpdate.removeAt(index)
                        }
                    }
                }
            }
        }
        return toUpdate
    }
}

private typealias ResultSyncVersions = Pair<List<Int>, List<Pair<Int, String>>>

class SyncGroupVersionsDbRequest(private val versions: Map<Int, Int>) :
    DbResponseRequest<ResultSyncVersions> {


    override val needsWrite: Boolean
        get() = false


    override fun process(
        database: Realm,
    ): ResultSyncVersions {
        val allKeys = versions.keys.toMutableList()
        val toRemove: List<RGroup> =
            database
                .where<RGroup>()
                .findAll()
                .filter { !allKeys.contains(it.identifier) }

        val toRemoveIds = toRemove.map { Pair(it.identifier, it.name) }
        val toUpdate = allKeys
        val listOfRGroupIds: RealmResults<RGroup> =
            database
                .where<RGroup>()
                .findAll()
        for (library in listOfRGroupIds) {
            if (library.syncState != ObjectSyncState.synced.name) {
                if (!toUpdate.contains(library.identifier)) {
                    toUpdate.add(library.identifier)
                }
            } else {
                val version = versions[library.identifier]
                val index = toUpdate.indexOfFirst { it == library.identifier }
                if (version != null && version == library.version && index != -1) {
                    toUpdate.removeAt(index)
                }
            }
        }
        return Pair(toUpdate, toRemoveIds)
    }
}
