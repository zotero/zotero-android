package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.FieldSchema
import javax.inject.Inject

class FieldSchemaMapper @Inject constructor() {

    fun fromJson(data: JsonObject): FieldSchema? {
        val field = data["field"]?.asString
        if (field == null) {
            return null
        }
        val baseField = data["baseField"]?.asString

        return FieldSchema(field = field, baseField = baseField)
    }
}