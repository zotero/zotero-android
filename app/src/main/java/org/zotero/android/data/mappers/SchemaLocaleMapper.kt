package org.zotero.android.data.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.SchemaLocale
import org.zotero.android.ktx.unmarshalMap
import javax.inject.Inject

class SchemaLocaleMapper @Inject constructor() {

    fun fromJson(data: JsonObject): SchemaLocale {
        val itemTypes = data["itemTypes"]?.unmarshalMap<String, String>() ?: emptyMap()
        val fields = data["fields"]?.unmarshalMap<String, String>() ?: emptyMap()
        val creatorTypes = data["creatorTypes"]?.unmarshalMap<String, String>() ?: emptyMap()
        return SchemaLocale(itemTypes = itemTypes, fields = fields, creatorTypes = creatorTypes)
    }
}