package org.zotero.android.screens.itemdetails.data

import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.sync.SchemaController
import java.util.Date

data class ItemDetailData(
    var title: String,
    var type: String,
    var isAttachment: Boolean,
    var localizedType: String,
    var creators: Map<String, ItemDetailCreator>,
    var creatorIds: List<String>,
    val fields: Map<String, ItemDetailField>,
    var fieldIds: List<String> = emptyList(),
    var abstract: String? = null,
    var dateModified: Date,
    val dateAdded: Date,
    var maxFieldTitleWidth: Double = 0.0,
    var maxNonemptyFieldTitleWidth: Double = 0.0
) {

    fun databaseFields(schemaController: SchemaController): List<ItemDetailField> {
        val allFields = this.fields.values.toMutableList()
        val titleKey = schemaController.titleKey(this.type)
        if (titleKey != null) {
            allFields.add(
                ItemDetailField(key = titleKey,
                baseField = if (titleKey != FieldKeys.Item.title) FieldKeys.Item.title else null,
                name = "",
                value = this.title,
                isTitle = true,
                isTappable = false)
            )
        }

        val abstract = this.abstract
        if (abstract != null) {
            allFields.add(
                ItemDetailField(key = FieldKeys.Item.abstractN,
                baseField = null,
                name = "",
                value = abstract,
                isTitle = false,
                isTappable = false)
            )
        }


        return allFields
    }

    fun deepCopy(
        title: String = this.title,
        type: String = this.type,
        isAttachment: Boolean = this.isAttachment,
        localizedType: String = this.localizedType,
        creators: Map<String, ItemDetailCreator> = this.creators,
        creatorIds: List<String> = this.creatorIds,
        fields: Map<String, ItemDetailField> = this.fields,
        fieldIds: List<String> = this.fieldIds,
        abstract: String? = this.abstract,
        dateModified: Date = this.dateModified,
        dateAdded: Date = this.dateAdded,
        maxFieldTitleWidth: Double = this.maxFieldTitleWidth,
        maxNonemptyFieldTitleWidth: Double = this.maxNonemptyFieldTitleWidth
    ): ItemDetailData {
        val clonedCreatorsMap = mutableMapOf<String, ItemDetailCreator>()
        for (cr in creators) {
            clonedCreatorsMap[cr.key] = cr.value.copy()
        }
        val clonedFieldsMap = mutableMapOf<String, ItemDetailField>()
        for (cf in fields) {
            clonedFieldsMap[cf.key] = cf.value.deepClone()
        }
        return copy(
            title = title,
            type = type,
            isAttachment = isAttachment,
            localizedType = localizedType,
            creators = clonedCreatorsMap,
            creatorIds = creatorIds.map { it },
            fields = clonedFieldsMap,
            fieldIds = fieldIds.map { it },
            abstract = abstract,
            dateModified = dateModified.clone() as Date,
            dateAdded = dateAdded.clone() as Date,
            maxFieldTitleWidth = maxFieldTitleWidth,
            maxNonemptyFieldTitleWidth = maxNonemptyFieldTitleWidth
        )

    }

    companion object {
        val empty: ItemDetailData
            get() {
            val date = Date()
            return ItemDetailData(title = "", type = "", isAttachment = false, localizedType = "", creators =  mapOf(), creatorIds = listOf(), fields = mapOf(), fieldIds = listOf(), abstract = null, dateModified = date, dateAdded = date,
            maxFieldTitleWidth = 0.0, maxNonemptyFieldTitleWidth = 0.0)
        }
    }
}