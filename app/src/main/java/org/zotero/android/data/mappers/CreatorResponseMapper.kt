package org.zotero.android.data.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.CreatorResponse
import javax.inject.Inject

class CreatorResponseMapper @Inject constructor() {

    fun fromJson(json: JsonObject): CreatorResponse {
        val creatorType = json["creatorType"].asString
        val firstName = json["firstName"]?.asString
        val lastName = json["lastName"]?.asString
        val name = json["name"]?.asString
        return CreatorResponse(
            creatorType = creatorType,
            firstName = firstName,
            lastName = lastName,
            name = name
        )

    }
}