package org.zotero.android.database.requests

import io.realm.Realm
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier

class ReadCollectionAndLibraryDbRequest(
    private val collectionId: CollectionIdentifier,
    private val libraryId: LibraryIdentifier,
): DbResponseRequest<Pair<Collection?, Library>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): Pair<Collection?, Library> {
        val library = ReadLibraryDbRequest(libraryId = this.libraryId).process(database)
        return when (val collectionIdLocal = this.collectionId) {
            is CollectionIdentifier.collection -> {
                val rCollection = ReadCollectionDbRequest(libraryId = this.libraryId, key = collectionIdLocal.key).process(database)
                    val collection = Collection.initWithCollection(objectS = rCollection, itemCount = 0)
                    collection to library
            }
            is CollectionIdentifier.custom -> {
                val collection = Collection.initWithCustomType(collectionIdLocal.type)
                return collection to library
            }
            else-> {
                null to library
            }
        }
    }
}