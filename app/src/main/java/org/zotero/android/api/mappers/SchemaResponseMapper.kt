package org.zotero.android.api.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.ItemSchema
import org.zotero.android.api.pojo.sync.SchemaLocale
import org.zotero.android.api.pojo.sync.SchemaResponse
import org.zotero.android.ktx.unmarshalMap
import javax.inject.Inject

class SchemaResponseMapper @Inject constructor(
    private val itemSchemaMapper: ItemSchemaMapper,
    private val schemaLocaleMapper: SchemaLocaleMapper,
    private val gson: Gson,
) {

    fun fromJson(data: JsonObject): SchemaResponse {
        val itemTypes = mutableMapOf<String, ItemSchema>()
        val typedData = data["itemTypes"]?.asJsonArray
        if (typedData != null) {
            for (data in typedData) {
                val itemSchema = itemSchemaMapper.fromJson(data.asJsonObject)
                if (itemSchema == null) {
                    continue
                }
                itemTypes[itemSchema.itemType] = itemSchema
            }
        }

        val locales = mutableMapOf<String, SchemaLocale>()

        val localeData: Map<String, JsonObject>? = data["locales"]?.unmarshalMap(gson)
        localeData?.forEach { data ->
            val fixedKey = data.key.replace("-", "_")
            locales[fixedKey] = schemaLocaleMapper.fromJson(data.value)
        }

        val version = data["version"]?.asInt ?: 0
        return SchemaResponse(version = version, itemSchemas = itemTypes, locales = locales )
    }
}