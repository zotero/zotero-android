package org.zotero.android.dashboard.data

import org.zotero.android.architecture.database.objects.FieldKeys
import org.zotero.android.sync.SchemaController
import java.util.Date
import java.util.UUID

data class ItemDetailData(
    var title: String,
    var type: String,
    var isAttachment: Boolean,
    var localizedType: String,
    var creators: Map<UUID, ItemDetailCreator>,
    var creatorIds: List<UUID>,
    var fields: Map<String, ItemDetailField>,
    var fieldIds: List<String>,
    var abstract: String? = null,
    var dateModified: Date,
    val dateAdded: Date,
    var maxFieldTitleWidth: Double = 0.0,
    var maxNonemptyFieldTitleWidth: Double = 0.0
) {

    fun databaseFields(schemaController: SchemaController): List<ItemDetailField> {
        var allFields = this.fields.values.toMutableList()
        val titleKey = schemaController.titleKey(this.type)
        if (titleKey != null) {
            allFields.add(ItemDetailField(key = titleKey,
                baseField = if (titleKey != FieldKeys.Item.title) FieldKeys.Item.title else null,
                name = "",
                value = this.title,
                isTitle = true,
                isTappable = false))
        }

        val abstract = this.abstract
        if (abstract != null) {
            allFields.add(ItemDetailField(key = FieldKeys.Item.abstractN,
                baseField = null,
                name = "",
                value = abstract,
                isTitle = false,
                isTappable = false))
        }


        return allFields
    }

    companion object {
        val empty: ItemDetailData get() {
            val date = Date()
            return ItemDetailData(title = "", type = "", isAttachment = false, localizedType = "", creators =  mapOf(), creatorIds = listOf(), fields = mapOf(), fieldIds = listOf(), abstract = null, dateModified = date, dateAdded = date,
            maxFieldTitleWidth = 0.0, maxNonemptyFieldTitleWidth = 0.0)
        }
    }
}