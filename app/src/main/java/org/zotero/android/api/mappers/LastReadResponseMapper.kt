package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import jakarta.inject.Inject
import org.zotero.android.api.pojo.sync.LastReadResponse
import org.zotero.android.api.pojo.sync.SettingKeyParser
import org.zotero.android.ktx.rounded
import org.zotero.android.sync.Parsing

class LastReadResponseMapper @Inject constructor() {
    fun fromJson(key: String, data: JsonObject): LastReadResponse? {
        if (!key.startsWith("lastRead_")) {
            return null
        }
        val (key, libraryId) = SettingKeyParser.parse(key = key)
        val value = data["value"].asLong
        val version = data["version"].asInt
        return LastReadResponse(
            key = key,
            libraryId = libraryId,
            value = value,
            version = version
        )
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