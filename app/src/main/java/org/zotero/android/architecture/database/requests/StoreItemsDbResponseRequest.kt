package org.zotero.android.architecture.database.requests

import com.google.gson.JsonObject
import io.realm.Realm
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
import org.zotero.android.architecture.database.objects.ItemTypes.Companion.case
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
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.StoreItemsResponse
import kotlin.reflect.KClass

class StoreItemsDbResponseRequest(
    val responses: List<ItemResponse>,
    val schemaController: SchemaController,
    val preferResponseData: Boolean
) : DbResponseRequest<StoreItemsResponse, StoreItemsResponse> {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm, clazz: KClass<StoreItemsResponse>?): StoreItemsResponse {
        var filenameChanges = mutableListOf<StoreItemsResponse.FilenameChange>()
        var errors = mutableListOf<StoreItemsResponse.Error>()

        //TODO continue implementation
        return StoreItemsResponse(emptyList(), emptyList())
    }
}

class StoreItemDbRequest(
    val response: ItemResponse,
    val schemaController: SchemaController,
    val preferRemoteData: Boolean,

): DbResponseRequest<Pair<
        RItem, StoreItemsResponse.FilenameChange?>,Pair<
        RItem, StoreItemsResponse.FilenameChange?>> {
    override val needsWrite: Boolean
        get() = true

    override fun process(
        database: Realm,
        clazz: KClass<Pair<RItem, StoreItemsResponse.FilenameChange?>>?
    ): Pair<RItem, StoreItemsResponse.FilenameChange?> {
        val libraryId = this.response.library.libraryId
        if (libraryId == null) {
            throw DbError.primaryKeyUnavailable
        }

        var item: RItem
        val existing = database.where<RItem>().key(this.response.key, libraryId).findFirst()
        if (existing != null) {
            item = existing
        } else {
            item = RItem()
            database.insertOrUpdate(item)
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
            item.deleteAllChanges(database =  database)
        }
        throw RuntimeException()
        //TODO continue implementation
    }

    companion object {

//        fun syncFields(data: ItemResponse, item: RItem, database: Realm, schemaController: SchemaController): StoreItemsResponse.FilenameChange? {
//            var oldName: String?
//            var newName: String?
//            var contentType: String?
//            val allFieldKeys = RItemChanges.fields.keys
//
//            let toRemove = item.fields.filter("NOT key IN %@", allFieldKeys.map({ $0.key }))
//            database.delete(toRemove)
//
//            var date: String?
//            var publisher: String?
//            var publicationTitle: String?
//            var sortIndex: String?
//            var md5: String?
//
//            for keyPair in allFieldKeys {
//                let value = data.fields[keyPair] ?? ""
//                var field: RItemField
//
//                // Backwards compatibility for fields that didn't store `annotationPosition` as `baseKey`. This is a precaution in case there is a field with the same key as a sub-field
//                // of `annotationPosition`. If there is just one key with the same name, we can look up by just key and update the `baseKey` value appropriately. Otherwise we have to look up both `key`
//                // and `baseKey`.
//                let keyCount: Int
//                        let existingFieldFilter: NSPredicate
//                        if let baseKey = keyPair.baseKey {
//                            keyCount = allFieldKeys.filter({ $0.key == keyPair.key }).count
//                            existingFieldFilter = keyCount == 1 ? .key(keyPair.key) : .key(keyPair.key, andBaseKey: baseKey)
//                        } else {
//                            keyCount = 0
//                            existingFieldFilter = .key(keyPair.key)
//                        }
//
//                if let existing = item.fields.filter(existingFieldFilter).first {
//                    if (existing.key == FieldKeys.Item.Attachment.filename || existing.baseKey == FieldKeys.Item.Attachment.filename) && existing.value != value {
//                        oldName = existing.value
//                        newName = value
//                    }
//                    if keyCount == 1 && existing.baseKey == nil {
//                        existing.baseKey = keyPair.baseKey
//                    }
//                    // Backend returns "<null>" for md5 and mtime for item which was submitted, but attachment has not yet been uploaded. Just ignore these values, we have correct values stored locally
//                    // and they'll be submitted on upload of attachment.
//                    if value != "<null>" || existing.value.isEmpty {
//                        existing.value = value
//                    }
//                    field = existing
//                } else {
//                    field = RItemField()
//                    field.key = keyPair.key
//                    field.baseKey = keyPair.baseKey ?? schemaController.baseKey(for: data.rawType, field: keyPair.key)
//                    field.value = value
//                    item.fields.append(field)
//                }
//
//                switch (field.key, field.baseKey) {
//                    case (FieldKeys.Item.title, _), (_, FieldKeys.Item.title):
//                    item.baseTitle = value
//                    case (FieldKeys.Item.note, _) where item.rawType == ItemTypes.note:
//                    item.baseTitle = NotePreviewGenerator.preview(from: value) ?? value
//                    case (FieldKeys.Item.date, _):
//                    date = value
//                    case (FieldKeys.Item.publisher, _), (_, FieldKeys.Item.publisher):
//                    publisher = value
//                    case (FieldKeys.Item.publicationTitle, _), (_, FieldKeys.Item.publicationTitle):
//                    publicationTitle = value
//                    case (FieldKeys.Item.Annotation.sortIndex, _):
//                    sortIndex = value
//                    case (FieldKeys.Item.Attachment.md5, _):
//                    if value != "<null>" {
//                        md5 = value
//                    }
//                    case (FieldKeys.Item.Attachment.contentType, _), (_, FieldKeys.Item.Attachment.contentType):
//                    contentType = value
//                    default: break
//                }
//            }
//
//            item.setDateFieldMetadata(date, parser: dateParser)
//            item.set(publisher: publisher)
//            item.set(publicationTitle: publicationTitle)
//            item.annotationSortIndex = sortIndex ?? ""
//            item.backendMd5 = md5 ?? ""
//
//            if let oldName = oldName, let newName = newName, let contentType = contentType {
//                return StoreItemsResponse.FilenameChange(key: item.key, oldName: oldName, newName: newName, contentType: contentType)
//            }
//            return nil
//        }

        fun syncRects(rects: List<List<Double>>, item: RItem, database: Realm) {
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

        private fun rects(rects: List<List<Double>>, itemRects: List<RRect>): Boolean {
            if (rects.size != itemRects.size) {
                return true
            }

            for (rect in rects) {
                if (itemRects.filter { it.minX == rect[0] && it.minY == rect[1] && it.maxX == rect[2] && it.maxY == rect[3] }.firstOrNull() == null) {
                    return true
                }
            }

            return false
        }

        fun syncPaths(paths: List<List<Double>>, item: RItem, database: Realm) {
            if (!paths(paths, item.paths)) {
                return
            }
            item.paths.deleteAllFromRealm()

            paths.forEachIndexed { idx, path ->
                val rPath = RPath()
                rPath.sortIndex = idx

                path.forEachIndexed { idy, value ->
                    val rCoordinate = RPathCoordinate()
                    rCoordinate.value = value
                    rCoordinate.sortIndex = idy
                    rPath.coordinates.add(rCoordinate)
                }

                item.paths.add(rPath)
            }
        }

        private fun paths(paths: List<List<Double>>, itemPaths: List<RPath>): Boolean {
            if (paths.size != itemPaths.size) {
                return true
            }

            val sortedPaths = itemPaths.sortedBy { it.sortIndex}

            paths.forEachIndexed { idx, path ->
                val itemPath = sortedPaths[idx]
                if (path.size != itemPath.coordinates.size) {
                    return true
                }

                val sortedCoordinates = itemPath.coordinates.sort("sortIndex")

                for (idy in 0..path.size) {
                    if (path[idy] != sortedCoordinates[idy]?.value) {
                        return true
                    }
                }
            }
            return false
        }

        fun syncParent(key: String?, libraryId: LibraryIdentifier, item: RItem, database: Realm) {
            if (key == null) {
                if (item.parent != null) {
                    item.parent = null
                }
                return
            }

            val parent: RItem
            val existing = database.where<RItem>().key(key, libraryId).findFirst()
            if (existing != null) {
                parent = existing
            } else {
                parent = RItem()
                parent.key = key
                parent.syncState = ObjectSyncState.dirty.name
                parent.libraryId = libraryId
                database.insertOrUpdate(parent)
            }

            item.parent = parent
        }

        fun syncCollections(keys: Set<String>, libraryId: LibraryIdentifier, item: RItem, database: Realm) {
            // Remove item from collections, which are not in the `keys` array anymore
            for (collection in item.collections.where().keyNotIn(keys).findAll()) {
                val index = collection.items.indexOf(item)
                if (index == -1) {
                    continue
                }
                collection.items.removeAt(index)
            }

            if (keys.isEmpty()) {
                return
            }

            var toCreateKeys = keys.toMutableList()
            val existingCollections = database.where<RCollection>().keys(keys, libraryId).findAll()

            for (collection in existingCollections) {
                if (collection.items.where().key(item.key).findFirst() == null) {
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

        fun syncTags(tags: List<TagResponse>, libraryId: LibraryIdentifier, item: RItem, database: Realm) {
            val toRemove = item.tags.where().tagNameNotIn(tags.map { it.tag }).findAll()

            val baseTagsToRemove = ReadBaseTagsToDeleteDbRequest<Any>(fromTags = toRemove).process(
                database = database
            )
            toRemove.deleteAllFromRealm()

            if (!baseTagsToRemove.isEmpty()) {
                database.where<RTag>().nameIn(baseTagsToRemove).findAll().deleteAllFromRealm()
            }
            if (tags.isEmpty()) {
                return
            }

                val allTags = database.where<RTag>()

                for (tag in tags) {
                    val existingA = item.tags.where().tagName(tag.tag).findFirst()
                    if (existingA != null) {
                        if (existingA.type != tag.type.name) {
                            existingA.type = tag.type.name
                        }
                        continue
                    }

                    val rTag : RTag
                    val existing = allTags.name(tag.tag, libraryId).findFirst()

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

        fun syncCreators(creators: List<CreatorResponse>, item: RItem, schemaController: SchemaController, database: Realm) {
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
                creator.primary = schemaController.creatorIsPrimary(creator.rawType, itemType =  item.rawType)
                item.creators.add(creator)
            }

            item.updateCreatorSummary()
        }

        fun syncRelations(relations: Map<String, JsonObject>, item: RItem, database: Realm) {
            val allKeys = relations.keys.toTypedArray()

            val toRemove = item.relations.filter { !allKeys.contains(it.type) }
            toRemove.forEach {
                it.deleteFromRealm()
            }

            for (key in allKeys) {
                val anyValue = relations[key]
                if (anyValue == null) {
                    continue
                }

                val valueRes: String = if (anyValue.isJsonPrimitive) {
                    anyValue.asString
                } else if (anyValue.isJsonArray) {
                    anyValue.asJsonArray.joinToString (separator = ";" )
                } else {
                    ""
                }

                val relation: RRelation
                val existing = item.relations.filter { it.type == key }.firstOrNull()
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
            val links = data.links
            if (links == null) {
                return
            }
            if (links.itself != null) {
                syncLink(data = links.itself, type =  LinkType.me.name, item = item, database =  database)
            }
            if (links.up != null) {
                syncLink(data = links.up, type = LinkType.up.name, item = item, database = database)
            }
            if (links.alternate != null) {
                syncLink(data =  links.alternate, type =  LinkType.alternate.name, item = item, database = database)
            }
            if (links.enclosure != null){
                syncLink(data =  links.enclosure, type =  LinkType.enclosure.name, item = item, database =  database)
            }
        }

        private fun syncLink(data: LinkResponse, type: String, item: RItem, database: Realm) {
            val link = RLink()
            link.type = type
            link.contentType = data.type ?: ""
            link.href = data.href
            link.title = data.title ?: ""
            link.length = data.length ?: 0
            item.links.add(link)
        }

        fun syncUsers(createdBy: UserResponse?, lastModifiedBy: UserResponse?, item: RItem, database: Realm) {
            if (item.createdBy?.isValid == false || item.createdBy?.identifier != createdBy?.id) {
                val user = if (item.createdBy?.isValid == false) null else item.createdBy

                item.createdBy = createdBy?.let { createUser(it, database) }

                if (user != null && user.createdBy.isEmpty() && user.modifiedBy.isEmpty()) {
                    user.deleteFromRealm()
                }

            }

            if (item.lastModifiedBy?.isValid == false || item.lastModifiedBy?.identifier != lastModifiedBy?.id) {
                val user = if (item.lastModifiedBy?.isValid == false) null else item.lastModifiedBy

                item.lastModifiedBy = lastModifiedBy?.let { createUser(it, database) }

                if (user != null && user.createdBy.isEmpty() && user.modifiedBy.isEmpty()) {
                    user.deleteFromRealm()
                }
            }
        }

        private fun createUser(response: UserResponse, database: Realm): RUser {
            val dbUser = database.where<RUser>().equalTo("identifier", response.id).findFirst()
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