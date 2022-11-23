package org.zotero.android.architecture.database.requests

import com.google.gson.JsonObject
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.CreatorResponse
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.LinkResponse
import org.zotero.android.api.pojo.sync.TagResponse
import org.zotero.android.api.pojo.sync.UserResponse
import org.zotero.android.architecture.database.DbError
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.FieldKeys
import org.zotero.android.architecture.database.objects.ItemTypes
import org.zotero.android.architecture.database.objects.LinkType
import org.zotero.android.architecture.database.objects.ObjectSyncState
import org.zotero.android.architecture.database.objects.RCollection
import org.zotero.android.architecture.database.objects.RCreator
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.RItemChanges
import org.zotero.android.architecture.database.objects.RItemField
import org.zotero.android.architecture.database.objects.RLink
import org.zotero.android.architecture.database.objects.RPath
import org.zotero.android.architecture.database.objects.RPathCoordinate
import org.zotero.android.architecture.database.objects.RRect
import org.zotero.android.architecture.database.objects.RRelation
import org.zotero.android.architecture.database.objects.RTag
import org.zotero.android.architecture.database.objects.RTypedTag
import org.zotero.android.architecture.database.objects.RUser
import org.zotero.android.architecture.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.NotePreviewGenerator
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.StoreItemsResponse
import java.util.Date
import kotlin.reflect.KClass

class StoreItemsDbResponseRequest(
    val responses: List<ItemResponse>,
    val schemaController: SchemaController,
    val preferResponseData: Boolean
) : DbResponseRequest<StoreItemsResponse, StoreItemsResponse> {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm, clazz: KClass<StoreItemsResponse>?): StoreItemsResponse {
        val filenameChanges = mutableListOf<StoreItemsResponse.FilenameChange>()
        val errors = mutableListOf<StoreItemsResponse.Error>()

        for (response in this.responses) {
            try {
                val (_, change) = StoreItemDbRequest(
                    response = response,
                    schemaController = this.schemaController,
                    preferRemoteData = this.preferResponseData
                ).process(database)
                if (change != null) {
                    filenameChanges.add(change)
                }
            } catch (e: Throwable) {
                val responseError = e as? StoreItemsResponse.Error
                if (responseError != null) {
                    errors.add(responseError)
                } else {
                    throw e
                }
            }
        }

        return StoreItemsResponse(changedFilenames = filenameChanges, conflicts = errors)
    }
}

class StoreItemDbRequest(
    val response: ItemResponse,
    val schemaController: SchemaController,
    val preferRemoteData: Boolean,
) : DbResponseRequest<
        Pair<RItem, StoreItemsResponse.FilenameChange?>,
        Pair<RItem, StoreItemsResponse.FilenameChange?>> {
    override val needsWrite: Boolean
        get() = true

    override fun process(
        database: Realm,
        clazz: KClass<Pair<RItem, StoreItemsResponse.FilenameChange?>>?
    ): Pair<RItem, StoreItemsResponse.FilenameChange?> {
        val libraryId = this.response.library.libraryId ?: throw DbError.primaryKeyUnavailable

        var item: RItem
        val existing = database
            .where<RItem>()
            .key(this.response.key, libraryId)
            .findFirst()
        if (existing != null) {
            item = existing
        } else {
            item = RItem()
            database.executeTransaction {
                item = database.copyToRealm(item)
            }
        }

        if (!this.preferRemoteData) {
            if (item.deleted) {
                throw StoreItemsResponse.Error.itemDeleted(this.response)
            }

            if (item.isChanged) {
                throw StoreItemsResponse.Error.itemChanged(this.response)
            }
        } else {
            database.executeTransaction {
                item.deleted = false
            }
            item.deleteAllChanges(database = database)
        }
        return update(
            item = item,
            libraryId = libraryId,
            this.response,
            schemaController = this.schemaController,
            database = database
        )
    }

    companion object {

        fun update(
            item: RItem,
            libraryId: LibraryIdentifier,
            response: ItemResponse,
            schemaController: SchemaController,
            database: Realm
        ): Pair<RItem, StoreItemsResponse.FilenameChange?> {
            var filenameChange: StoreItemsResponse.FilenameChange? = null
            database.executeTransaction {
                item.key = response.key
                item.rawType = response.rawType
                item.localizedType =
                    schemaController.localizedItemType(itemType = response.rawType) ?: ""
                item.inPublications = response.inPublications
                item.version = response.version
                item.trash = response.isTrash
                item.dateModified = response.dateModified
                item.dateAdded = response.dateAdded
                item.syncState = ObjectSyncState.synced.name
                item.syncRetries = 0
                item.lastSyncDate = Date()
                item.changeType = UpdatableChangeType.sync.name
                item.libraryId = libraryId

                filenameChange = syncFields(
                    data = response,
                    item = item,
                    database = database,
                    schemaController = schemaController
                )
                syncParent(
                    key = response.parentKey,
                    libraryId = libraryId,
                    item = item,
                    database = database
                )
                syncCollections(
                    keys = response.collectionKeys,
                    libraryId = libraryId,
                    item = item,
                    database = database
                )
                syncTags(
                    tags = response.tags,
                    libraryId = libraryId,
                    item = item,
                    database = database
                )
                syncCreators(
                    creators = response.creators,
                    item = item,
                    schemaController = schemaController,
                    database = database
                )
                syncRelations(
                    relations = response.relations,
                    item = item,
                    database = database
                )
                syncLinks(
                    data = response,
                    item = item,
                    database = database
                )
                syncUsers(
                    createdBy = response.createdBy,
                    lastModifiedBy = response.lastModifiedBy,
                    item = item,
                    database = database
                )
                syncRects(
                    rects = response.rects ?: emptyList(),
                    item = item,
                    database = database
                )
                syncPaths(
                    paths = response.paths ?: emptyList(),
                    item = item,
                    database = database
                )
                item.updateDerivedTitles()
            }
            return item to filenameChange
        }

        fun syncFields(
            data: ItemResponse,
            item: RItem,
            database: Realm,
            schemaController: SchemaController
        ): StoreItemsResponse.FilenameChange? {
            var oldName: String? = null
            var newName: String? = null
            var contentType: String? = null
            val allFieldKeys = data.fields.keys.toTypedArray()

            val toRemove = item.fields
                .where()
                .not()
                .`in`("key", allFieldKeys.map { it.key }.toTypedArray())
            toRemove.findAll().deleteAllFromRealm()

            var date: String? = null
            var publisher: String? = null
            var publicationTitle: String? = null
            var sortIndex: String? = null
            var md5: String? = null

            for (keyPair in allFieldKeys) {
                val value = data.fields[keyPair] ?: ""
                var field: RItemField

                val keyCount: Int
                val existingFieldFilter: RealmQuery<RItemField>
                val baseKey = keyPair.baseKey
                if (baseKey != null) {
                    keyCount = allFieldKeys.filter { it.key == keyPair.key }.size
                    if (keyCount == 1) {
                        existingFieldFilter = item.fields
                            .where()
                            .key(keyPair.key)
                    } else {
                        existingFieldFilter =
                            item.fields
                                .where()
                                .key(keyPair.key, andBaseKey = baseKey)
                    }
                } else {
                    keyCount = 0
                    existingFieldFilter = item.fields
                        .where()
                        .key(keyPair.key)
                }

                val existing = existingFieldFilter.findFirst()

                if (existing != null) {
                    if ((existing.key == FieldKeys.Item.Attachment.filename
                                || existing.baseKey == FieldKeys.Item.Attachment.filename)
                        && existing.value != value
                    ) {
                        oldName = existing.value
                        newName = value
                    }
                    if (keyCount == 1 && existing.baseKey == null) {
                        existing.baseKey = keyPair.baseKey
                    }
                    if (value != "<null>" || existing.value.isEmpty()) {
                        existing.value = value
                    }
                    field = existing
                } else {
                    field = RItemField()
                    field.key = keyPair.key
                    field.baseKey = keyPair.baseKey ?: schemaController.baseKey(
                        data.rawType,
                        field = keyPair.key
                    )
                    field.value = value
                    item.fields.add(field)
                }

                when {
                    field.key == FieldKeys.Item.title || field.baseKey == FieldKeys.Item.title -> {
                        item.baseTitle = value
                    }
                    field.key == FieldKeys.Item.note && item.rawType == ItemTypes.note -> {
                        item.baseTitle = NotePreviewGenerator.preview(value) ?: value
                    }
                    field.key == FieldKeys.Item.date -> {
                        date = value
                    }
                    field.key == FieldKeys.Item.publisher || field.baseKey == FieldKeys.Item.publisher -> {
                        publisher = value
                    }
                    field.key == FieldKeys.Item.publicationTitle || field.baseKey == FieldKeys.Item.publicationTitle -> {
                        publicationTitle = value
                    }
                    field.key == FieldKeys.Item.Annotation.sortIndex -> {
                        sortIndex = value
                    }
                    field.key == FieldKeys.Item.Attachment.md5 -> {
                        if (value != "<null>") {
                            md5 = value
                        }
                    }
                    field.key == FieldKeys.Item.Attachment.contentType || field.baseKey == FieldKeys.Item.Attachment.contentType -> {
                        contentType = value
                    }
                }
            }

//            item.setDateFieldMetadata //TODO
            item.setP(publisher = publisher)
            item.setPT(publicationTitle = publicationTitle)
            item.annotationSortIndex = sortIndex ?: ""
            item.backendMd5 = md5 ?: ""

            if (oldName != null && newName != null && contentType != null) {
                return StoreItemsResponse.FilenameChange(
                    key = item.key,
                    oldName = oldName,
                    newName = newName,
                    contentType = contentType
                )
            }
            return null
        }

        fun syncRects(
            rects: List<List<Double>>,
            item: RItem,
            database: Realm
        ) {
            if (!rects(rects, item.rects)) {
                return
            }
            item.rects.deleteAllFromRealm()
            for (rect in rects) {
                val rRect = RRect()
                rRect.minX = rect[0]
                rRect.minY = rect[1]
                rRect.maxX = rect[2]
                rRect.maxY = rect[3]
                item.rects.add(rRect)
            }
        }

        private fun rects(
            rects: List<List<Double>>,
            itemRects: List<RRect>
        ): Boolean {
            if (rects.size != itemRects.size) {
                return true
            }

            for (rect in rects) {
                if (itemRects.firstOrNull {
                        it.minX == rect[0]
                                && it.minY == rect[1]
                                && it.maxX == rect[2]
                                && it.maxY == rect[3]
                    } == null) {
                    return true
                }
            }

            return false
        }

        fun syncPaths(
            paths: List<List<Double>>,
            item: RItem,
            database: Realm
        ) {
            if (!paths(paths, item.paths)) {
                return
            }
            item.paths.deleteAllFromRealm()
            paths.forEachIndexed { idx, path ->
                val rPath = database.createEmbeddedObject(RPath::class.java, item, "paths")
                rPath.sortIndex = idx

                path.forEachIndexed { idy, value ->
                    val rCoordinate = RPathCoordinate()
                    rCoordinate.value = value
                    rCoordinate.sortIndex = idy
                    rPath.coordinates!!.add(rCoordinate)
                }

                //TODO propably not needed
//                item.paths.add(rPath)
            }
        }

        private fun paths(
            paths: List<List<Double>>,
            itemPaths: List<RPath>
        ): Boolean {
            if (paths.size != itemPaths.size) {
                return true
            }
            val sortedPaths = itemPaths.sortedBy { it.sortIndex }
            paths.forEachIndexed { idx, path ->
                val itemPath = sortedPaths[idx]
                if (path.size != itemPath.coordinates!!.size) {
                    return true
                }
                val sortedCoordinates = itemPath.coordinates.sort("sortIndex")
                for (idy in 0..path.size) {
                    if (path[idy]
                        != sortedCoordinates[idy]?.value
                    ) {
                        return true
                    }
                }
            }
            return false
        }

        fun syncParent(
            key: String?,
            libraryId: LibraryIdentifier,
            item: RItem,
            database: Realm
        ) {
            if (key == null) {
                if (item.parent != null) {
                    item.parent = null
                }
                return
            }

            var parent: RItem
            val existing = database
                .where<RItem>()
                .key(key, libraryId)
                .findFirst()
            if (existing != null) {
                parent = existing
            } else {
                parent = RItem()
                parent.key = key
                parent.syncState = ObjectSyncState.dirty.name
                parent.libraryId = libraryId
                parent = database.copyToRealm(parent)
            }
            item.parent = parent
        }

        fun syncCollections(
            keys: Set<String>,
            libraryId: LibraryIdentifier,
            item: RItem,
            database: Realm
        ) {
            // Remove item from collections, which are not in the `keys` array anymore
            for (collection in item.collections!!
                .where()
                .keyNotIn(keys)
                .findAll()) {
                val index = collection.items.indexOf(item)
                if (index == -1) {
                    continue
                }
                collection.items.removeAt(index)
            }
            if (keys.isEmpty()) {
                return
            }
            val toCreateKeys = keys.toMutableList()
            val existingCollections = database.where<RCollection>().keys(keys, libraryId).findAll()
            for (collection in existingCollections) {
                if (collection.items
                        .where()
                        .key(item.key)
                        .findFirst() == null
                ) {
                    collection.items.add(item)
                }
                toCreateKeys.remove(collection.key)
            }

            for (key in toCreateKeys) {
                val collection = RCollection()
                collection.key = key
                collection.syncState = ObjectSyncState.dirty.name
                collection.libraryId = libraryId
                database.insertOrUpdate(collection)
                collection.items.add(item)
            }
        }

        fun syncTags(
            tags: List<TagResponse>,
            libraryId: LibraryIdentifier,
            item: RItem,
            database: Realm
        ) {
            val toRemove = item.tags!!
                .where()
                .tagNameNotIn(tags.map { it.tag })
                .findAll()

            val baseTagsToRemove = ReadBaseTagsToDeleteDbRequest<Any>(fromTags = toRemove)
                .process(database = database)
            toRemove.deleteAllFromRealm()

            if (baseTagsToRemove.isNotEmpty()) {
                database
                    .where<RTag>()
                    .nameIn(baseTagsToRemove)
                    .findAll()
                    .deleteAllFromRealm()
            }
            if (tags.isEmpty()) {
                return
            }

            val allTags = database.where<RTag>()

            for (tag in tags) {
                val existingA = item.tags
                    .where()
                    .tagName(tag.tag)
                    .findFirst()
                if (existingA != null) {
                    if (existingA.type != tag.type.name) {
                        existingA.type = tag.type.name
                    }
                    continue
                }

                val rTag: RTag
                val existing = allTags
                    .name(tag.tag, libraryId)
                    .findFirst()

                if (existing != null) {
                    rTag = existing
                } else {
                    rTag = RTag()
                    rTag.name = tag.tag
                    rTag.libraryId = libraryId
                    database.insertOrUpdate(rTag)
                }

                val rTypedTag = RTypedTag()
                rTypedTag.type = RItemChanges.type.name
                database.insertOrUpdate(rTypedTag)
                rTypedTag.item = item
                rTypedTag.tag = rTag
            }
        }

        fun syncCreators(
            creators: List<CreatorResponse>,
            item: RItem,
            schemaController: SchemaController,
            database: Realm
        ) {
            item.creators.forEach {
                it.deleteFromRealm()
            }

            for (objectS in creators.withIndex()) {

                val firstName = objectS.value.firstName ?: ""
                val lastName = objectS.value.lastName ?: ""
                val name = objectS.value.name ?: ""

                val creator = RCreator()
                creator.rawType = objectS.value.creatorType
                creator.firstName = firstName
                creator.lastName = lastName
                creator.name = name
                creator.orderId = objectS.index
                creator.primary =
                    schemaController.creatorIsPrimary(
                        creatorType = creator.rawType,
                        itemType = item.rawType
                    )
                item.creators.add(creator)
            }
            item.updateCreatorSummary()
        }

        fun syncRelations(
            relations: JsonObject,
            item: RItem,
            database: Realm
        ) {
            val allKeys = relations.keySet().toTypedArray()

            val toRemove = item.relations.filter { !allKeys.contains(it.type) }
            toRemove.forEach {
                it.deleteFromRealm()
            }

            for (key in allKeys) {
                val anyValue = relations[key] ?: continue

                val valueRes: String = if (anyValue.isJsonPrimitive) {
                    anyValue.asString
                } else if (anyValue.isJsonArray) {
                    anyValue.asJsonArray.joinToString(separator = ";")
                } else {
                    ""
                }

                val relation: RRelation
                val existing = item.relations.firstOrNull { it.type == key }
                if (existing != null) {
                    relation = existing
                } else {
                    relation = RRelation()
                    relation.type = key
                    item.relations.add(relation)
                }

                relation.urlString = valueRes
            }
        }

        fun syncLinks(data: ItemResponse, item: RItem, database: Realm) {
            item.links.deleteAllFromRealm()
            val links = data.links ?: return
            if (links.itself != null) {
                syncLink(
                    data = links.itself,
                    type = LinkType.me.name,
                    item = item,
                    database = database
                )
            }
            if (links.up != null) {
                syncLink(
                    data = links.up,
                    type = LinkType.up.name,
                    item = item,
                    database = database
                )
            }
            if (links.alternate != null) {
                syncLink(
                    data = links.alternate,
                    type = LinkType.alternate.name,
                    item = item,
                    database = database
                )
            }
            if (links.enclosure != null) {
                syncLink(
                    data = links.enclosure,
                    type = LinkType.enclosure.name,
                    item = item,
                    database = database
                )
            }
        }

        private fun syncLink(
            data: LinkResponse,
            type: String,
            item: RItem,
            database: Realm
        ) {
            val link = RLink()
            link.type = type
            link.contentType = data.type ?: ""
            link.href = data.href
            link.title = data.title ?: ""
            link.length = data.length ?: 0
            item.links.add(link)
        }

        fun syncUsers(
            createdBy: UserResponse?,
            lastModifiedBy: UserResponse?,
            item: RItem,
            database: Realm
        ) {
            if (item.createdBy?.isValid == false || item.createdBy?.identifier != createdBy?.id) {
                val user = if (item.createdBy?.isValid == false) {
                    null
                } else {
                    item.createdBy
                }
                item.createdBy = createdBy?.let { createUser(it, database) }
                if (user != null && user.createdBy.isEmpty() && user.modifiedBy.isEmpty()) {
                    user.deleteFromRealm()
                }
            }

            if (item.lastModifiedBy?.isValid == false || item.lastModifiedBy?.identifier != lastModifiedBy?.id) {
                val user = if (item.lastModifiedBy?.isValid == false) {
                    null
                } else {
                    item.lastModifiedBy
                }
                item.lastModifiedBy = lastModifiedBy?.let { createUser(it, database) }
                if (user != null && user.createdBy.isEmpty() && user.modifiedBy.isEmpty()) {
                    user.deleteFromRealm()
                }
            }
        }

        private fun createUser(response: UserResponse, database: Realm): RUser {
            val dbUser = database
                .where<RUser>()
                .equalTo("identifier", response.id)
                .findFirst()
            if (dbUser != null) {
                if (dbUser.name != response.name) {
                    dbUser.name = response.name
                }
                if (dbUser.username != response.username) {
                    dbUser.username = response.username
                }
                return dbUser
            }

            val user = RUser()
            user.identifier = response.id
            user.name = response.name
            user.username = response.username
            database.insertOrUpdate(user)
            return user
        }
    }
}