
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults
import org.zotero.android.architecture.database.objects.ObjectSyncState
import org.zotero.android.architecture.database.objects.RGroup

class SyncGroupVersionsDbRequest(private val versions: Map<Int, Int>) :
    DbResponseRequest<Pair<List<Int>, List<Pair<Int, String>>>> {


    override val needsWrite: Boolean
        get() = false


    override fun process(database: Realm): Pair<List<Int>, List<Pair<Int, String>>> {
        val allKeys = versions.keys.toMutableList()

        val toRemove: List<RGroup> =
            database.query<RGroup>()
                .find().filter { !allKeys.contains(it.identifier) }

        val toRemoveIds = toRemove.map { Pair(it.identifier, it.name) }

        val toUpdate = allKeys

        val listOfRGroupIds: RealmResults<RGroup> =
            database.query<RGroup>().find()

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
