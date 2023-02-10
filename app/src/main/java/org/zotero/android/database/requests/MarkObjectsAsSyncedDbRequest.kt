package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmModel
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.CollectionResponse
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.SearchResponse
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RCollectionChanges
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RSearch
import org.zotero.android.database.objects.RSearchChanges
import org.zotero.android.database.objects.Syncable
import org.zotero.android.database.objects.Updatable
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController

class MarkObjectsAsSyncedDbRequest(
    val libraryId: LibraryIdentifier,
    val keys: List<String>,
    val changeUuids: Map<String, List<String>>,
    val version: Int,
    var clazz: Class<out RealmModel>,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val objects = database.where(clazz).keys(this.keys, this.libraryId).findAll()
        for (objectS in objects) {
            objectS as Syncable
            objectS as Updatable
            if (objectS.version != this.version) {
                objectS.version = this.version
            }

            objectS.changeType = UpdatableChangeType.syncResponse.name
            val uuids = this.changeUuids[objectS.key]
            if (uuids != null) {
                objectS.deleteChanges(uuids = uuids, database = database)
            }
        }
    }
}

class MarkCollectionAsSyncedAndUpdateDbRequest(
    val libraryId: LibraryIdentifier,
    val response: CollectionResponse,
    val changeUuids: List<String>,
): DbRequest {

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val collection = database.where<RCollection>().key(this.response.key, this.libraryId).findFirst()
        if (collection == null) {
            return
        }

        collection.deleteChanges(uuids = this.changeUuids, database = database)
        updateUnchangedData(collection, this.response, database = database)
    }

    private fun updateUnchangedData(collection: RCollection, response: CollectionResponse, database: Realm) {
        val localChanges = collection.changedFields

        if (localChanges.isEmpty()) {
            StoreCollectionsDbRequest.update(collection = collection, response = this.response, libraryId = this.libraryId, database = database)
            collection.changeType = UpdatableChangeType.syncResponse.name
            return
        }

        collection.version = response.version
        collection.trash = response.data.isTrash
        collection.changeType = UpdatableChangeType.syncResponse.name

        if (!localChanges.contains(RCollectionChanges.nameS)) {
            collection.name = response.data.name
        }

        if (!localChanges.contains(RCollectionChanges.parent)) {
            StoreCollectionsDbRequest.sync(parentCollection = response.data.parentCollection,
                libraryId = this.libraryId, collection = collection, database = database)
        }
    }
}

class MarkItemAsSyncedAndUpdateDbRequest (
    val libraryId: LibraryIdentifier,
    val response: ItemResponse,
    val changeUuids: List<String>,
    val schemaController: SchemaController,
): DbRequest {

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.response.key, this.libraryId).findFirst()
        if (item == null) {
            return
        }

        item.deleteChanges(uuids = this.changeUuids, database = database)
        updateUnchangedData(item, this.response, database = database)

        val parent = item.parent
        if (parent != null) {
            parent.version = parent.version
        }
    }

    private fun updateUnchangedData(item: RItem, response: ItemResponse, database: Realm) {
        val localChanges = item.changedFields

        if (localChanges.isEmpty()) {
            StoreItemDbRequest.update(item = item, libraryId = this.libraryId, response,
                schemaController = this.schemaController, database = database)
            item.changeType = UpdatableChangeType.syncResponse.name
            return
        }

        item.version = response.version
        item.dateModified = response.dateModified
        item.inPublications = response.inPublications
        item.changeType = UpdatableChangeType.syncResponse.name

        if (!localChanges.contains(RItemChanges.trash)) {
            item.trash = response.isTrash
        }

        if (!localChanges.contains(RItemChanges.parent) && item.parent?.key != response.parentKey) {
            StoreItemDbRequest.syncParent(key = response.parentKey, libraryId = this.libraryId, item = item, database = database)
        }

        // If type changed remotely and we have local field changes, we ignore the type change, so that the type and fields stay in sync (different types can have different fields).
        if (!localChanges.contains(RItemChanges.type) && item.rawType != response.rawType && !localChanges.contains(RItemChanges.fields)) {
            item.rawType = response.rawType
            item.localizedType = schemaController.localizedItemType(itemType = response.rawType) ?: response.rawType
        }

        if (!localChanges.contains(RItemChanges.fields)) {
            StoreItemDbRequest.syncFields(data = response, item = item, database = database, schemaController = this.schemaController)
        }

        if (!localChanges.contains(RItemChanges.collections)) {
            StoreItemDbRequest.syncCollections(keys = response.collectionKeys, libraryId = this.libraryId, item = item, database = database)
        }

        if (!localChanges.contains(RItemChanges.tags)) {
            StoreItemDbRequest.syncTags(tags = response.tags, libraryId = this.libraryId, item = item, database = database)
        }

        if (!localChanges.contains(RItemChanges.creators)) {
            StoreItemDbRequest.syncCreators(creators = response.creators, item = item,
                schemaController = this.schemaController, database = database)
        }

        if (!localChanges.contains(RItemChanges.relations)) {
            StoreItemDbRequest.syncRelations(relations = response.relations, item = item, database = database)
        }

        if (!localChanges.contains(RItemChanges.rects)) {
            StoreItemDbRequest.syncRects(rects = response.rects ?: emptyList(), item, database = database)
        }

        if (!localChanges.contains(RItemChanges.paths)) {
            StoreItemDbRequest.syncPaths(paths = response. paths ?: emptyList(), item, database = database)
        }

        item.updateDerivedTitles()
    }
}

class MarkSearchAsSyncedAndUpdateDbRequest(
    val libraryId:LibraryIdentifier,
    val response:SearchResponse,
    val changeUuids:List<String>,
):DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database:Realm) {
        val search = database.where<RSearch>().key(this.response.key, this.libraryId).findFirst()
        if (search == null) {
            return
        }

        search.deleteChanges(uuids = this.changeUuids, database = database)
        updateUnchangedData(search, response = this.response, database = database)
    }

    private fun updateUnchangedData(search: RSearch, response: SearchResponse, database: Realm) {
        val localChanges = search.changedFields

        if (localChanges.isEmpty()) {
            StoreSearchesDbRequest.update(search = search, response = this.response,
                libraryId = this.libraryId, database = database)
            search.changeType = UpdatableChangeType.syncResponse.name
            return
        }

        search.trash = response.data.isTrash
        search.version = response.version
        search.changeType = UpdatableChangeType.syncResponse.name

        if (!localChanges.contains(RSearchChanges.nameS)) {
            search.name = response.data.name
        }

        if (!localChanges.contains(RSearchChanges.conditions)) {
            StoreSearchesDbRequest.sync(conditions = response.data.conditions, search = search, database = database)
        }
    }
}
