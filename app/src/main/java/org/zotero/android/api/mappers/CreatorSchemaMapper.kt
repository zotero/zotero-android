package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.CreatorSchema
import javax.inject.Inject

class CreatorSchemaMapper @Inject constructor() {

    fun fromJson(data: JsonObject): CreatorSchema? {
        val creatorType = data["creatorType"]?.asString
        if (creatorType == null) {
            return null
        }
        val primary = data["primary"]?.asBoolean ?: false

        return CreatorSchema(creatorType = creatorType, primary = primary)
    }
}