package org.zotero.android.database.requests

import com.google.gson.JsonObject
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.androidx.text.strippedRichTextTags
import org.zotero.android.api.pojo.sync.CreatorResponse
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.LinkResponse
import org.zotero.android.api.pojo.sync.TagResponse
import org.zotero.android.api.pojo.sync.UserResponse
import org.zotero.android.database.DbError
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.AllItemsDbRowCreator
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.LinkType
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RCreator
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RLink
import org.zotero.android.database.objects.RPath
import org.zotero.android.database.objects.RPathCoordinate
import org.zotero.android.database.objects.RRect
import org.zotero.android.database.objects.RRelation
import org.zotero.android.database.objects.RTag
import org.zotero.android.database.objects.RTypedTag
import org.zotero.android.database.objects.RUser
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.NotePreviewGenerator
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.StoreItemsResponse
import timber.log.Timber
import java.util.Date
import java.util.UUID

class StoreItemsDbResponseRequest(
    val responses: List<ItemResponse>,
    val schemaController: SchemaController,
    val dateParser: DateParser,
    val preferResponseData: Boolean,
    val denyIncorrectCreator: Boolean,
) : DbResponseRequest<StoreItemsResponse> {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): StoreItemsResponse {
        val filenameChanges = mutableListOf<StoreItemsResponse.FilenameChange>()
        val errors = mutableListOf<StoreItemsResponse.Error>()

        for (response in this.responses) {
            try {
                val (_, change) = StoreItemDbRequest(
                    response = response,
                    schemaController = this.schemaController,
                    dateParser = this.dateParser,
                    preferRemoteData = this.preferResponseData,
                    denyIncorrectCreator = this.denyIncorrectCreator,
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
    val dateParser: DateParser,
    val denyIncorrectCreator: Boolean,
) : DbResponseRequest<
        Pair<RItem, StoreItemsResponse.FilenameChange?>> {
    override val needsWrite: Boolean
        get() = true

    override fun process(
        database: Realm,
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
            item = database.createObject<RItem>()
        }

        if (!this.preferRemoteData) {
            if (item.deleted) {
                throw StoreItemsResponse.Error.itemDeleted(this.response)
            }

            if (item.isChanged) {
                throw StoreItemsResponse.Error.itemChanged(this.response)
            }
        } else {
            item.deleted = false
            item.deleteAllChanges(database = database)
            item.attachmentNeedsSync = false
        }
        return update(
            item = item,
            libraryId = libraryId,
            response = this.response,
            denyIncorrectCreator = this.denyIncorrectCreator,
            schemaController = this.schemaController,
            dateParser = dateParser,
            database = database
        )
    }

    companion object {

        fun update(
            item: RItem,
            libraryId: LibraryIdentifier,
            response: ItemResponse,
            denyIncorrectCreator: Boolean,
            schemaController: SchemaController,
            dateParser: DateParser,
            database: Realm
        ): Pair<RItem, StoreItemsResponse.FilenameChange?> {
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

            val filenameChange: StoreItemsResponse.FilenameChange? = syncFields(
                data = response,
                item = item,
                database = database,
                schemaController = schemaController,
                dateParser = dateParser,
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
                denyIncorrectCreator = denyIncorrectCreator,
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
            AllItemsDbRowCreator.createOrUpdate(item, database)
            return item to filenameChange
        }

        fun syncFields(
            data: ItemResponse,
            item: RItem,
            database: Realm,
            schemaController: SchemaController,
            dateParser: DateParser,
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
                    if (value != "null" || existing.value.isEmpty()) {
                        existing.value = value
                    }
                    field = existing
                } else {
                    field = database.createEmbeddedObject(RItemField::class.java, item, "fields")
                    field.key = keyPair.key
                    field.baseKey = keyPair.baseKey ?: schemaController.baseKey(
                        data.rawType,
                        field = keyPair.key
                    )
                    field.value = value
                }

                when {
                    field.key == FieldKeys.Item.title || field.baseKey == FieldKeys.Item.title -> {
                        item.baseTitle = value
                    }

                    field.key == FieldKeys.Item.note && item.rawType == ItemTypes.note -> {
                        try {
                            item.baseTitle = NotePreviewGenerator.preview(value) ?: ""
                        } catch (e: Exception) {
                            Timber.e(
                                e,
                                "StoreItemsDbResponseRequest: unable to set baseTitle after value was processed by NotePreviewGenerator. Original value = ${
                                    value.take(210)
                                }"
                            )
                        }
                        item.htmlFreeContent = value.ifEmpty { null }
                    }
                    field.key == FieldKeys.Item.Annotation.comment && item.rawType == ItemTypes.annotation -> {
                        item.htmlFreeContent = if(value.isEmpty()) null else value.strippedRichTextTags
                    }

                    field.key == FieldKeys.Item.Annotation.type && item.rawType == ItemTypes.annotation -> {
                        item.annotationType = value
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
                        if (value != "null") {
                            md5 = value
                        }
                    }
                    field.key == FieldKeys.Item.Attachment.contentType || field.baseKey == FieldKeys.Item.Attachment.contentType -> {
                        contentType = value
                    }
                }
            }

            if (date != null) {
                item.setDateFieldMetadata(date, parser = dateParser)
            } else {
                item.clearDateFieldMedatada()
            }
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
                val rRect = database.createEmbeddedObject(RRect::class.java, item, "rects")
                rRect.minX = rect[0]
                rRect.minY = rect[1]
                rRect.maxX = rect[2]
                rRect.maxY = rect[3]
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
                    val rCoordinate = database.createEmbeddedObject(RPathCoordinate::class.java, rPath, "coordinates")
                    rCoordinate.value = value
                    rCoordinate.sortIndex = idy
//                    rPath.coordinates.add(rCoordinate)
                }
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
                if (path.size != itemPath.coordinates.size) {
                    return true
                }
                val sortedCoordinates = itemPath.coordinates.sort("sortIndex")
                for (idy in path.indices) {
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
                parent = database.createObject<RItem>()
                parent.key = key
                parent.syncState = ObjectSyncState.dirty.name
                parent.libraryId = libraryId
            }
            item.parent = parent
            AllItemsDbRowCreator.createOrUpdate(parent, database)
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
                val collection = database.createObject<RCollection>()
                collection.key = key
                collection.syncState = ObjectSyncState.dirty.name
                collection.libraryId = libraryId
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

            val baseTagsToRemove = ReadBaseTagsToDeleteDbRequest(fromTags = toRemove)
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

            val allTags = database.where<RTag>().findAll()

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
                    .where()
                    .name(tag.tag, libraryId)
                    .findFirst()

                if (existing != null) {
                    rTag = existing
                } else {
                    rTag = database.createObject<RTag>()
                    rTag.name = tag.tag
                    rTag.updateSortName()
                    rTag.libraryId = libraryId
                }

                val rTypedTag = database.createObject<RTypedTag>()
                rTypedTag.type = tag.type.name
                rTypedTag.item = item
                rTypedTag.tag = rTag
            }
        }

        fun syncCreators(
            creators: List<CreatorResponse>,
            item: RItem,
            denyIncorrectCreator: Boolean,
            schemaController: SchemaController,
            database: Realm
        ) {
            when(item.rawType) {
                ItemTypes.annotation, ItemTypes.attachment, ItemTypes.note -> {
                    // These item types don't support creators, so `validCreators` would always be empty.
                    return
                }
                else -> {
                    //no-op
                }
            }

            item.creators.deleteAllFromRealm()

            val validCreators = schemaController.creators(item.rawType)
            if (validCreators == null || validCreators.isEmpty()) {
                Timber.w("StoreItemsDbResponseRequest: can't find valid creators for item type ${item.rawType}. Skipping creators.")
                throw StoreItemsResponse.Error.noValidCreators(key = item.key, itemType = item.rawType)
            }

            for ((idx, objectS) in creators.withIndex()) {
                val firstName = objectS.firstName ?: ""
                val lastName = objectS.lastName ?: ""
                val name = objectS.name ?: ""

                val creator = database.createEmbeddedObject(RCreator::class.java, item, "creators")
                creator.uuid = UUID.randomUUID().toString()

                if (validCreators.any { it.creatorType == objectS.creatorType }) {
                    creator.rawType = objectS.creatorType
                } else if (denyIncorrectCreator) {
                    throw StoreItemsResponse.Error.invalidCreator(
                        key = item.key,
                        creatorType = objectS.creatorType
                    )
                } else {
                    val primaryCreator = validCreators.firstOrNull { it.primary }
                    if (primaryCreator != null) {
                        Timber.e("StoreItemsDbResponseRequest: creator type '${objectS.creatorType}' isn't valid for ${item.rawType} - changing to primary creator")
                        creator.rawType = primaryCreator.creatorType
                    } else {
                        Timber.e("StoreItemsDbResponseRequest: creator type '${objectS.creatorType}' isn't valid for ${item.rawType} and primary creator doesn't exist - changing to first valid creator")
                        creator.rawType = validCreators[0].creatorType
                    }
                }

                creator.firstName = firstName
                creator.lastName = lastName
                creator.name = name
                creator.orderId = idx
                creator.primary =
                    schemaController.creatorIsPrimary(
                        creatorType = creator.rawType,
                        itemType = item.rawType
                    )
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
                    relation = database.createEmbeddedObject(RRelation::class.java, item, "relations")
                    relation.type = key
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
            val link = database.createEmbeddedObject(RLink::class.java, item, "links")
            link.type = type
            link.contentType = data.type ?: ""
            link.href = data.href
            link.title = data.title ?: ""
            link.length = data.length ?: 0
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
                if (user != null && user.createdBy!!.isEmpty() && user.modifiedBy!!.isEmpty()) {
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
                if (user != null && user.createdBy!!.isEmpty() && user.modifiedBy!!.isEmpty()) {
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

            val user = database.createObject<RUser>(response.id)
            user.name = response.name
            user.username = response.username
            return user
        }
    }
}