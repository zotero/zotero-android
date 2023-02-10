package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.LinkResponse
import javax.inject.Inject

class LinkResponseMapper @Inject constructor(){

    fun fromJson(json: JsonObject): LinkResponse {
        val href = json["href"].asString
        val type = json["type"]?.asString
        val title = json["title"]?.asString
        val length = json["length"]?.asInt
        return LinkResponse(href = href, type = type, title = title, length = length)
    }
}