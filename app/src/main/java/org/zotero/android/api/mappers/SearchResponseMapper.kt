package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.SearchResponse
import javax.inject.Inject

class SearchResponseMapper @Inject constructor(
    private val libraryResponseMapper: LibraryResponseMapper,
    private val linksResponseMapper: LinksResponseMapper,
    private val searchResponseDataMapper: SearchResponseDataMapper
) {
    fun fromJson(response: JsonObject): SearchResponse {
        val key = response["key"].asString
        val library = response["library"].asJsonObject
        val data = response["data"].asJsonObject
        val libraryParsed = libraryResponseMapper.fromJson(library)
        val links = response["links"]?.asJsonObject?.let { linksResponseMapper.fromJson(it) }
        val version = response["version"].asInt
        val dataParsed = searchResponseDataMapper.fromJson(response = data, key = key)

        return SearchResponse(
            key = key,
            library = libraryParsed,
            links = links,
            version = version,
            data = dataParsed
        )
    }

}
