package org.zotero.android.data.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.LibraryResponse
import org.zotero.android.api.pojo.sync.LinksResponse
import javax.inject.Inject

class LibraryResponseMapper @Inject constructor(private val linksResponseMapper: LinksResponseMapper) {

    fun fromJson(json: JsonObject): LibraryResponse {
        val id = json["id"].asInt
        val name = json["name"].asString
        val type = json["type"].asString
        val links: LinksResponse? = json["links"]?.asJsonObject?.let { linksResponseMapper.fromJson(it) }
        return LibraryResponse(id = id, name = name, type = type , links = links)

    }
}