package org.zotero.android.architecture.database.requests
import org.zotero.android.architecture.database.DbResponseRequest
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.architecture.database.objects.ObjectSyncState
import org.zotero.android.architecture.database.objects.RGroup
import kotlin.reflect.KClass

private typealias Result = Pair<List<Int>, List<Pair<Int, String>>>

class SyncGroupVersionsDbRequest(private val versions: Map<Int, Int>) :
    DbResponseRequest<Result, Result> {


    override val needsWrite: Boolean
        get() = false




    override fun process(
        database: Realm,
        clazz: KClass<Result>?
    ): Result {
        val allKeys = versions.keys.toMutableList()

        val toRemove: List<RGroup> =
            database.where<RGroup>().findAll().filter { !allKeys.contains(it.identifier) }

        val toRemoveIds = toRemove.map { Pair(it.identifier, it.name) }

        val toUpdate = allKeys

        val listOfRGroupIds: RealmResults<RGroup> =
            database.where<RGroup>().findAll()

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
