package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.CollectionResponse
import javax.inject.Inject

class CollectionResponseMapper @Inject constructor(
    private val libraryResponseMapper: LibraryResponseMapper,
    private val linksResponseMapper: LinksResponseMapper,
    private val collectionResponseDataMapper: CollectionResponseDataMapper,
) {
    fun fromJson(response: JsonObject): CollectionResponse {
        val library = response["library"].asJsonObject
        val data = response["data"].asJsonObject
        val key = response["key"].asString
        val libraryParsed = libraryResponseMapper.fromJson(library)
        val links = response["links"]?.asJsonObject?.let { linksResponseMapper.fromJson(it) }
        val version = response["version"].asInt
        val dataParsed = collectionResponseDataMapper.fromJson(response = data, key = key)
        return CollectionResponse(
            key = key,
            library = libraryParsed,
            links = links,
            version = version,
            data = dataParsed
        )
    }

}
