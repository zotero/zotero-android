package org.zotero.android.data.mappers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.ItemSchema
import javax.inject.Inject

class ItemSchemaMapper @Inject constructor(
    private val fieldSchemaMapper: FieldSchemaMapper,
    private val creatorSchemaMapper: CreatorSchemaMapper
) {

    fun fromJson(data: JsonObject): ItemSchema? {
        val itemType = data["itemType"]?.asString
        if (itemType == null) {
            return null
        }
        val fieldData = data["fields"]?.asJsonArray ?: JsonArray()
        val fields = fieldData.mapNotNull { fieldSchemaMapper.fromJson(it.asJsonObject) }
        val creatorData = data["creatorTypes"]?.asJsonArray ?: JsonArray()
        val creatorTypes = creatorData.mapNotNull { creatorSchemaMapper.fromJson(it.asJsonObject) }

        return ItemSchema(itemType = itemType, fields = fields, creatorTypes = creatorTypes)
    }
}