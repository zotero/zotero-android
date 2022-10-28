package org.zotero.android.data.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.SettingsResponse
import javax.inject.Inject

class SettingsResponseMapper @Inject constructor(
    private val tagColorsResponseMapper: TagColorsResponseMapper,
    private val pageIndicesResponseMapper: PageIndicesResponseMapper
) {

    fun fromJson(json: JsonObject): SettingsResponse {
        val tagColors =
            json["tagColors"]?.asJsonObject?.let { tagColorsResponseMapper.fromJson(it) }
        val pageIndices = pageIndicesResponseMapper.fromJson(json)

        return SettingsResponse(tagColors = tagColors, pageIndices = pageIndices)
    }
}