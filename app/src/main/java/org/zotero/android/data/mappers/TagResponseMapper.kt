package org.zotero.android.data.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.TagResponse
import org.zotero.android.architecture.database.objects.RTypedTag
import javax.inject.Inject

class TagResponseMapper @Inject constructor(){

    fun fromJson(json: JsonObject): TagResponse {
        val rawType = json ["type"]?.asInt ?: 0
        val type = RTypedTag.Kind.from(rawType)!!
        return TagResponse(tag = json["tag"].asString, type = type)
    }
}