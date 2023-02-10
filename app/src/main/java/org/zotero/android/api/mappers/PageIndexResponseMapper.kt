package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.PageIndexResponse
import javax.inject.Inject

class PageIndexResponseMapper @Inject constructor(){

    fun fromJson(key: String, dictionary: JsonObject): PageIndexResponse? {

        if (!key.contains("lastPageIndex")) {
            return null
        }

        val (key, libraryId) = PageIndexResponse.parse(key = key)

        val value = dictionary["value"].asInt
        val version = dictionary["version"].asInt

        return PageIndexResponse(key = key, value = value, version = version, libraryId = libraryId)
    }
}