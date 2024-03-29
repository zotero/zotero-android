package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.CollectionResponse
import org.zotero.android.database.DbError
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier
import java.util.Date

class StoreCollectionsDbRequest(
    val response: List<CollectionResponse>,
) : DbRequest {

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        for (data in this.response) {
            store(data = data, database)
        }
    }

    private fun store(data: CollectionResponse, database: Realm) {
            val libraryId = data.library.libraryId ?: throw DbError.primaryKeyUnavailable

            val collection: RCollection
            val existing =
                database
                    .where<RCollection>()
                    .key(data.key, libraryId = libraryId)
                    .findFirst()
            if (existing != null) {
                collection = existing
            } else {
                collection = database.createObject<RCollection>()
                collection.collapsed = true
            }

            if (collection.deleted) {
                for (item in collection.items) {
                    item.trash = false
                    item.deleted = false
                }
            }
            collection.deleted = false
            collection.deleteAllChanges(database = database)

            // Update local instance with remote values
            update(
                collection = collection,
                response = data,
                libraryId = libraryId,
                database = database
            )
    }

    companion object {
        fun sync(
            parentCollection: String?,
            libraryId: LibraryIdentifier,
            collection: RCollection,
            database: Realm
        ) {
            collection.parentKey = null
            val key = parentCollection ?: return
            val parent: RCollection
            val existing = database
                .where<RCollection>()
                .key(key, libraryId = libraryId)
                .findFirst()
            if (existing != null) {
                parent = existing
            } else {
                parent = database.createObject<RCollection>()
                parent.key = key
                parent.syncState = ObjectSyncState.dirty.name
                parent.libraryId = libraryId

            }
            collection.parentKey = parent.key
        }

        fun update(
            collection: RCollection,
            response: CollectionResponse,
            libraryId: LibraryIdentifier,
            database: Realm
        ) {
            collection.key = response.key
            collection.name = response.data.name
            collection.version = response.version
            collection.syncState = ObjectSyncState.synced.name
            collection.syncRetries = 0
            collection.lastSyncDate = Date()
            collection.changeType = UpdatableChangeType.sync.name
            collection.libraryId = libraryId
            collection.trash = response.data.isTrash

            sync(
                parentCollection = response.data.parentCollection,
                libraryId = libraryId,
                collection = collection,
                database = database
            )
        }
    }
}