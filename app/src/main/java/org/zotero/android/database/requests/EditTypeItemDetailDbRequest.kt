package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.screens.itemdetails.data.ItemDetailField
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.LibraryIdentifier

class EditTypeItemDetailDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val type: String,
    private val fields: List<ItemDetailField>,
    private val creatorIds: List<String>,
    private val creators: Map<String, ItemDetailCreator>,
    private val dateParser: DateParser,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(key, libraryId).findFirst() ?: return

        item.rawType = type

        val changes: MutableList<RItemChanges> = mutableListOf(RItemChanges.type)

        update(fields = fields, item = item, changes = changes, database = database)
        update(creatorIds = creatorIds, creators =  creators, item = item, changes = changes, database = database)
        item.changes.add(RObjectChange.create(changes = changes))
    }


    private fun update(
        fields: List<ItemDetailField>,
        item: RItem,
        changes: MutableList<RItemChanges>,
        database: Realm
    ) {
        val toRemove = item.fields.where().keyNotIn(fields.map{ it.key }).findAll()
        if (!toRemove.isEmpty()) {
            changes.add(RItemChanges.fields)
        }
        for (field in toRemove) {
            when {
                field.key == FieldKeys.Item.note || field.key == FieldKeys.Item.Annotation.comment -> {
                    item.htmlFreeContent = null
                }
                field.key == FieldKeys.Item.title || field.baseKey == FieldKeys.Item.title -> {
                    item.set("")
                }
                field.key == FieldKeys.Item.date || field.baseKey == FieldKeys.Item.date -> {
                    item.clearDateFieldMedatada()
                }
                field.key == FieldKeys.Item.publisher || field.baseKey == FieldKeys.Item.publisher -> {
                    item.setP(null)
                }
                field.key == FieldKeys.Item.publicationTitle || field.baseKey == FieldKeys.Item.publicationTitle -> {
                    item.setPT(null)
                }
            }
        }
        toRemove.deleteAllFromRealm()

        for (field in fields) {
            if (item.fields.where().key(field.key).findFirst() != null) {
                continue
            }


            val rField = database.createEmbeddedObject(RItemField::class.java, item, "fields")
            rField.key = field.key
            rField.baseKey = field.baseField
            rField.value = field.value
            rField.changed = true
            changes.add(RItemChanges.fields)

            when {
                field.key == FieldKeys.Item.title || field.baseField == FieldKeys.Item.title -> {
                    item.set(field.value)
                }
                field.key == FieldKeys.Item.date || field.baseField == FieldKeys.Item.date -> {
                    item.setDateFieldMetadata(field.value, parser = dateParser)
                }
                field.key == FieldKeys.Item.publisher || field.baseField == FieldKeys.Item.publisher -> {
                    item.setP(field.value)
                }
                field.key == FieldKeys.Item.publicationTitle || field.baseField == FieldKeys.Item.publicationTitle -> {
                    item.setPT(field.value)
                }

            }
        }

    }
    private fun update(
        creatorIds: List<String>,
        creators: Map<String, ItemDetailCreator>,
        item: RItem,
        changes: MutableList<RItemChanges>,
        database: Realm
    ) {
        val toRemove = item.creators.where()
            .not()
            .`in`("uuid", creatorIds.toTypedArray())
            .findAll()

        if (!toRemove.isEmpty()) {
            changes.add(RItemChanges.creators)
        }
        toRemove.deleteAllFromRealm()

        for (creatorId in creatorIds) {
            val creator = creators[creatorId] ?: continue
            val rCreator =
                item.creators.where().equalTo("uuid", creatorId).findFirst() ?: continue
            if (rCreator.rawType == creator.type) {
                continue
            }
            rCreator.rawType = creator.type
            changes.add(RItemChanges.creators)
        }

        if (changes.contains(RItemChanges.creators)) {
            item.updateCreatorSummary()
        }
    }
}