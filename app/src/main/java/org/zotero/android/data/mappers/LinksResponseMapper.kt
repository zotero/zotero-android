package org.zotero.android.data.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.LinkResponse
import org.zotero.android.api.pojo.sync.LinksResponse
import javax.inject.Inject

class LinksResponseMapper @Inject constructor(private val linkResponseMapper: LinkResponseMapper) {

    fun fromJson(json: JsonObject): LinksResponse {
        val itself: LinkResponse? = json["self"]?.asJsonObject?.let { linkResponseMapper.fromJson(it) }
        val alternate: LinkResponse? = json["alternate"]?.asJsonObject?.let { linkResponseMapper.fromJson(it) }
        val up: LinkResponse? = json["up"]?.asJsonObject?.let { linkResponseMapper.fromJson(it) }
        val enclosure: LinkResponse? = json["enclosure"]?.asJsonObject?.let { linkResponseMapper.fromJson(it) }

        return LinksResponse(itself = itself, alternate = alternate, up = up, enclosure = enclosure)

    }
}