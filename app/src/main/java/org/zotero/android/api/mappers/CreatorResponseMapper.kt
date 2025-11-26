package org.zotero.android.api.mappers

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.CreatorResponse
import javax.inject.Inject

class CreatorResponseMapper @Inject constructor() {

    fun fromJson(json: JsonObject): CreatorResponse {
        val creatorType = json["creatorType"].asString
        val firstName = stringOrNull(json["firstName"])
        val lastName = stringOrNull(json["lastName"])
        val name = stringOrNull(json["name"])
        return CreatorResponse(
            creatorType = creatorType,
            firstName = firstName,
            lastName = lastName,
            name = name
        )

    }

    private fun stringOrNull(objectS: JsonElement?): String? {
        if (objectS == null) {
            return null
        }
        if (objectS is JsonNull) {
            return null
        }
        return objectS.asString

    }
}