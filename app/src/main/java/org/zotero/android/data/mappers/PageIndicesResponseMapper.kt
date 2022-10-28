package org.zotero.android.data.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.PageIndicesResponse
import javax.inject.Inject

class PageIndicesResponseMapper @Inject constructor(private val pageIndexResponseMapper: PageIndexResponseMapper) {

    fun fromJson(json: JsonObject): PageIndicesResponse {
        val indices = json.entrySet()
            .mapNotNull { pageIndexResponseMapper.fromJson(it.key, it.value.asJsonObject) }
        return PageIndicesResponse(indices = indices)
    }
}