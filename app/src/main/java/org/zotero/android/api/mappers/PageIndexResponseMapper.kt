package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.PageIndexResponse
import org.zotero.android.ktx.rounded
import org.zotero.android.sync.Parsing
import javax.inject.Inject

class PageIndexResponseMapper @Inject constructor(){

    fun fromJson(key: String, dictionary: JsonObject): PageIndexResponse? {
        if (!key.contains("lastPageIndex")) {
            return null
        }
        try {
            val (key, libraryId) = PageIndexResponse.parse(key = key)

            val version = dictionary["version"].asInt

            return PageIndexResponse(
                key = key,
                value = parseValue(dictionary),
                version = version,
                libraryId = libraryId
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseValue(dictionary: JsonObject): String {
        try {
            return "${dictionary["value"].asInt}"
        } catch (e: Exception) {
        }
        try {
            return "${dictionary["value"].asDouble.rounded(1)}"
        } catch (e: Exception) {
        }
        try {
            return dictionary["value"].asString
        } catch (e: Exception) {
        }
        throw Parsing.Error.missingKey("value")
    }
}