package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.LastReadAloudPositionResponse
import org.zotero.android.api.pojo.sync.LastReadAloudPositionsResponse
import org.zotero.android.api.pojo.sync.SettingKeyParser
import org.zotero.android.speech.data.ReadAloudResumePosition
import timber.log.Timber
import javax.inject.Inject

class LastReadAloudPositionResponseMapper @Inject constructor() {

    fun fromJson(json: JsonObject): LastReadAloudPositionsResponse {
        val positions = json.entrySet().mapNotNull { (key, value) ->
            (value as? JsonObject)?.let { fromJson(key, it) }
        }
        return LastReadAloudPositionsResponse(positions = positions)
    }

    private fun fromJson(key: String, dictionary: JsonObject): LastReadAloudPositionResponse? {
        if (!key.startsWith("lastReadAloudPosition_")) {
            return null
        }
        try {
            val (key, libraryId) = SettingKeyParser.parse(key = key)
            val rawValue = dictionary["value"].asJsonObject
            val value = ReadAloudResumePosition.from(rawValue)
            if (value == null) {
                Timber.e("LastReadAloudPositionResponseMapper: unknown value for key $key - $rawValue")
                return null
            }
            return LastReadAloudPositionResponse(
                key = key,
                value = value,
                version = dictionary["version"].asInt,
                libraryId = libraryId,
            )
        } catch (e: Exception) {
            Timber.e(e, "LastReadAloudPositionResponseMapper: can't parse $key")
            return null
        }
    }
}