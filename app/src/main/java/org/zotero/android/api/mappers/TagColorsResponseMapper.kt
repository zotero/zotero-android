package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.TagColorsResponse
import javax.inject.Inject

class TagColorsResponseMapper @Inject constructor(private val tagColorResponseMapper: TagColorResponseMapper) {

    fun fromJson(json: JsonObject): TagColorsResponse {
        val array = json["value"].asJsonArray
        val value = array.map { tagColorResponseMapper.fromJson(it.asJsonObject) }
        return TagColorsResponse(value = value)
    }
}