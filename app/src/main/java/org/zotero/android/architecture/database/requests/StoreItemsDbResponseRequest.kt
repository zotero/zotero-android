package org.zotero.android.architecture.database.requests

import com.google.gson.JsonObject
import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.CreatorResponse
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.LinkResponse
import org.zotero.android.api.pojo.sync.UserResponse
import org.zotero.android.architecture.database.DbError
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.LinkType
import org.zotero.android.architecture.database.objects.RCreator
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.RLink
import org.zotero.android.architecture.database.objects.RRelation
import org.zotero.android.architecture.database.objects.RUser
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

        fun sync(creators: List<CreatorResponse>, item: RItem, schemaController: SchemaController, database: Realm) {
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

        fun sync(relations: Map<String, JsonObject>, item: RItem, database: Realm) {
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