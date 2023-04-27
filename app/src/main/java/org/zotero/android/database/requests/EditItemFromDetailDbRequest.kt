package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RCreator
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.screens.itemdetails.data.ItemDetailData
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController

class EditItemFromDetailDbRequest(
    val libraryId: LibraryIdentifier,
    val itemKey: String,
    val data: ItemDetailData,
    val snapshot: ItemDetailData,
    private val schemaController: SchemaController,
    private val dateParser: DateParser,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.itemKey, this.libraryId).findFirst()
        if (item == null) {
            return
        }
        var changes = mutableListOf<RItemChanges>()

        val typeChanged = this.data.type != item.rawType
        if (typeChanged) {
            item.rawType = this.data.type
            changes.add(RItemChanges.type)
        }
        item.dateModified = this.data.dateModified

        updateCreators(this.data, snapshot = this.snapshot, item = item, changes = changes, database = database)
        updateFields(this.data, snapshot = this.snapshot, item = item, changes = changes, typeChanged = typeChanged, database = database)

        if (!changes.isEmpty()) {
            item.updateDerivedTitles()
            item.changeType = UpdatableChangeType.user.name
            item.changes.add(RObjectChange.create(changes = changes))
        }
    }

    private fun updateCreators(data: ItemDetailData, snapshot: ItemDetailData, item: RItem, changes: MutableList<RItemChanges>, database: Realm) {
        if (data.creatorIds == snapshot.creatorIds && data.creators == snapshot.creators) { return }

        item.creators.deleteAllFromRealm()

        for ((offset, creatorId) in data.creatorIds.withIndex()) {
            val creator = data.creators[creatorId]
            if (creator == null) {
                continue
            }

            val rCreator = database.createEmbeddedObject(RCreator::class.java, item, "creators")
            rCreator.rawType = creator.type
            rCreator.orderId = offset
            rCreator.primary = creator.primary

            when (creator.namePresentation) {
                ItemDetailCreator.NamePresentation.full -> {
                    rCreator.name = creator.fullName
                    rCreator.firstName = ""
                    rCreator.lastName = ""
                }
                ItemDetailCreator.NamePresentation.separate -> {
                    rCreator.name = ""
                    rCreator.firstName = creator.firstName
                    rCreator.lastName = creator.lastName
                }
            }
        }
        item.updateCreatorSummary()
        changes.add(RItemChanges.creators)
    }

    private fun updateFields(
        data: ItemDetailData,
        snapshot: ItemDetailData,
        item: RItem,
        changes: MutableList<RItemChanges>,
        typeChanged: Boolean,
        database: Realm
    ) {
        val allFields = data.databaseFields(schemaController = this.schemaController)
        val snapshotFields = this.snapshot.databaseFields(schemaController = this.schemaController)

        var fieldsDidChange = false

        if (typeChanged) {
            val fieldKeys = allFields.map { it.key }
            val toRemove = item.fields.where().keyNotIn(fieldKeys).findAll()

            toRemove.forEach { field ->
                if (field.key == FieldKeys.Item.date) {
                    item.setDateFieldMetadata(null, parser = this.dateParser)
                } else if (field.key == FieldKeys.Item.publisher || field.baseKey == FieldKeys.Item.publisher) {
                    item.setP(null)
                } else if (field.key == FieldKeys.Item.publicationTitle || field.baseKey == FieldKeys.Item.publicationTitle) {
                    item.setPT(null)
                }
            }
            toRemove.deleteAllFromRealm()
            fieldsDidChange = !toRemove.isEmpty()
        }

        for ((offset, field) in allFields.withIndex()) {
            if (!typeChanged && !(field.value != snapshotFields[offset].value)) {
                continue
            }

            var fieldToChange: RItemField? = null
            val existing = item.fields.where().key(field.key).findFirst()

            if (existing != null) {
                fieldToChange = if (field.value != existing.value) existing else null
            } else {
                val rField = database.createEmbeddedObject(RItemField::class.java, item, "fields")
                rField.key = field.key
                rField.baseKey = field.baseField
                fieldToChange = rField
            }
            val rField = fieldToChange

            if (rField != null) {
                rField.value = field.value
                rField.changed = true

                if (field.isTitle) {
                    item.baseTitle = field.value
                } else if (field.key == FieldKeys.Item.date) {
                    item.setDateFieldMetadata(field.value, parser = this.dateParser)
                } else if (field.key == FieldKeys.Item.publisher || field.baseField == FieldKeys.Item.publisher) {
                    item.setP(field.value)
                } else if (field.key == FieldKeys.Item.publicationTitle || field.baseField == FieldKeys.Item.publicationTitle) {
                    item.setPT(field.value)
                }

                fieldsDidChange = true
            }
        }

        if (fieldsDidChange) {
            changes.add(RItemChanges.fields)
        }
    }

}