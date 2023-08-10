package org.zotero.android.api.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.FailedUpdateResponse
import org.zotero.android.api.pojo.sync.UpdatesResponse
import org.zotero.android.ktx.unmarshalMap
import org.zotero.android.sync.Parsing
import javax.inject.Inject

class UpdatesResponseMapper @Inject constructor(private val gson: Gson) {

    fun fromJson(dictionary: JsonObject, keys: List<String?>): UpdatesResponse {
        if (!dictionary.isJsonObject) {
            throw Parsing.Error.notDictionary
        }

        val successful = dictionary["success"]?.unmarshalMap<String, String>(gson) ?: emptyMap()
        val successfulJsonObjects = dictionary["successful"]?.unmarshalMap<String, JsonObject>(gson) ?: emptyMap()
        val unchanged = dictionary["unchanged"]?.unmarshalMap<String, String>(gson) ?: emptyMap()

        val failed = dictionary["failed"]?.unmarshalMap<String, JsonObject>(gson) ?: emptyMap()
        val failedResult = failed.map {entry ->
            val key = entry.key
            val value = entry.value
            val keyResult: String? = key.toInt().let { idx ->
                if (idx >= keys.size) {
                    return@let null
                }
                return@let keys[idx]
            }
            mapFailedUpdateResponse(data = value, key = keyResult)
        }
        return UpdatesResponse(
            successful = successful,
            successfulJsonObjects = successfulJsonObjects,
            unchanged = unchanged,
            failed = failedResult
        )
    }
    private fun mapFailedUpdateResponse(data: JsonObject, key: String?): FailedUpdateResponse {
        val keyResult = key ?: data["key"]?.asString
        val code = data["code"]?.asInt ?: 0
        val message = data["message"]?.asString ?: ""
        return FailedUpdateResponse(
            key = keyResult,
            code = code,
            message = message
        )
    }
}