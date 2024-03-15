package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.screens.share.data.RecentData
import org.zotero.android.sync.Collection
import org.zotero.android.sync.LibraryIdentifier

class ReadRecentCollections(val excluding: Pair<String, LibraryIdentifier>?) :
    DbResponseRequest<List<RecentData>> {

    private val limit = 5

    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): List<RecentData> {
        val collections = database.where<RCollection>().sort("lastUsed", Sort.DESCENDING).findAll()
        if (collections.isEmpty()) {
            return emptyList()
        }

        val recent = mutableListOf<RecentData>()
        for (rCollection in collections) {
            val libraryId = rCollection.libraryId
            if (libraryId == null) {
                break
            }

            val library = ReadLibraryDbRequest(libraryId = libraryId).process(database)
            val excludingPair = this.excluding
            if (excludingPair != null && rCollection.key == excludingPair.first && library.identifier == excludingPair.second) {
                continue
            }

            val collection = Collection.initWithCollection(objectS = rCollection, itemCount = 0)

            recent.add(RecentData(collection = collection, library = library, isRecent = true))

            if (recent.size == this.limit) {
                break
            }
        }
        return recent
    }
}