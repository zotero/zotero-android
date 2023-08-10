package org.zotero.android.api.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.SchemaLocale
import org.zotero.android.ktx.unmarshalMap
import javax.inject.Inject

class SchemaLocaleMapper @Inject constructor(private val gson: Gson) {

    fun fromJson(data: JsonObject): SchemaLocale {
        val itemTypes = data["itemTypes"]?.unmarshalMap<String, String>(gson) ?: emptyMap()
        val fields = data["fields"]?.unmarshalMap<String, String>(gson) ?: emptyMap()
        val creatorTypes = data["creatorTypes"]?.unmarshalMap<String, String>(gson) ?: emptyMap()
        return SchemaLocale(itemTypes = itemTypes, fields = fields, creatorTypes = creatorTypes)
    }
}