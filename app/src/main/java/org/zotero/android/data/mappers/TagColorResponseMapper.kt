package org.zotero.android.data.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.TagColorResponse
import javax.inject.Inject

class TagColorResponseMapper @Inject constructor(){

    fun fromJson(json: JsonObject): TagColorResponse {
        val name = json ["name"].asString
        val color = json ["color"].asString
        return TagColorResponse(name = name, color = color)
    }
}