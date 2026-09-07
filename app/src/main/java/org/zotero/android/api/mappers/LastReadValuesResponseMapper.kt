package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.LastReadValuesResponse
import javax.inject.Inject

class LastReadValuesResponseMapper @Inject constructor(private val lastReadResponseMapper: LastReadResponseMapper) {

    fun fromJson(json: JsonObject): LastReadValuesResponse {
        val values = json.entrySet()
            .mapNotNull { lastReadResponseMapper.fromJson(it.key, it.value.asJsonObject) }
        return LastReadValuesResponse(values = values)
    }
}