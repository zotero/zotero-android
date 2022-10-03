package org.zotero.android.data.mappers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.LibraryResponse
import org.zotero.android.api.pojo.sync.LinksResponse
import org.zotero.android.api.pojo.sync.UserResponse
import org.zotero.android.architecture.database.objects.ItemTypes
import org.zotero.android.formatter.iso8601DateFormatV2
import org.zotero.android.ktx.unmarshalList
import java.util.Date
import javax.inject.Inject

class ItemResponseMapper @Inject constructor(
    private val creatorResponseMapper: CreatorResponseMapper,
    private val libraryResponseMapper: LibraryResponseMapper,
    private val linksResponseMapper: LinksResponseMapper,
    private val tagResponseMapper: TagResponseMapper,
    private val userResponseMapper: UserResponseMapper
) {
    fun fromJson(json: JsonObject): ItemResponse {
        val data = json["data"].asJsonObject
        val key: String = json["key"].asString
        val itemType = data["itemType"].asString


        val library = libraryResponseMapper.fromJson(json["library"].asJsonObject)

        val links = json["links"]?.asJsonObject?.let { linksResponseMapper.fromJson(it) }
        val meta = json["meta"]?.asJsonObject
        val parsedDate = meta?.get("parsedDate")?.asString
        val createdByData = meta?.get("createdByUser")?.asJsonObject
        val createdBy = createdByData?.let { userResponseMapper.fromJson(it) }
        val lastModifiedByData = meta?.get("lastModifiedByUser")?.asJsonObject
        val lastModifiedBy = lastModifiedByData?.let { userResponseMapper.fromJson(it) }
        val version: Int = json["version"].asInt

        return when (itemType) {
            ItemTypes.annotation -> {
                initAnnotation(
                    key = key,
                    createdBy = createdBy,
                    lastModifiedBy = lastModifiedBy,
                    library = library,
                    links = links,
                    parsedDate = parsedDate,
                    version = version,
                    data = data
                )
            }
            else -> {
                initItem(
                    key = key,
                    data = data,
                    version = version,
                    parsedDate = parsedDate,
                    links = links,
                    library = library,
                    lastModifiedBy = lastModifiedBy,
                    createdBy = createdBy,
                    rawType = itemType
                )
            }
        }
    }


    // Init any item type except annotation
    private fun initItem(
        key: String,
        rawType: String,
        library: LibraryResponse,
        links: LinksResponse?,
        parsedDate: String?,
        createdBy: UserResponse?,
        lastModifiedBy: UserResponse?,
        version: Int,
        data: JsonObject
    ): ItemResponse {
        val dateAdded = data["dateAdded"]?.asString
        val dateModified = data["dateModified"]?.asString
        val tags = data["tags"]?.asJsonArray ?: JsonArray()
        val creators = data["creators"]?.asJsonArray ?: JsonArray()

        val collectionKeys =
            data["collections"]?.unmarshalList<String>()?.toSet() ?: emptySet<String>()
        val parentKey = data["parentItem"]?.asString
        val dateAddedParsed = dateAdded?.let { iso8601DateFormatV2.parse(it) } ?: Date()
        val dateModifiedParsed = dateModified?.let { iso8601DateFormatV2.parse(it) } ?: Date()
        val isTrash = data["deleted"]?.asBoolean ?: (data["deleted"]?.asInt == 1)
        val tagsParsed = tags.map { tagResponseMapper.fromJson(it.asJsonObject) }
        val creatorsParsed = creators.map { creatorResponseMapper.fromJson(it.asJsonObject) }
        val relations = data["relations"]?.asJsonObject ?: JsonObject()
        val inPublications =
            data["inPublications"]?.asBoolean ?: (data["inPublications"]?.asInt == 1)
        //TODO parse fiels, rects, paths

        return ItemResponse(
            rawType = rawType,
            collectionKeys = collectionKeys,
            createdBy = createdBy,
            creators = creatorsParsed,
            relations = relations,
            dateAdded = dateAddedParsed,
            dateModified = dateModifiedParsed,
            inPublications = inPublications,
            isTrash = isTrash,
            key = key,
            parentKey = parentKey,
            lastModifiedBy = lastModifiedBy,
            library = library,
            links = links,
            parsedDate = parsedDate,
            tags = tagsParsed,
            version = version
        )
    }

    private fun initAnnotation(
        key: String,
        library: LibraryResponse,
        links: LinksResponse?,
        parsedDate: String?,
        createdBy: UserResponse?,
        lastModifiedBy: UserResponse?,
        version: Int,
        data: JsonObject
    ): ItemResponse {
        val dateAdded = data["dateAdded"]?.asString
        val dateModified = data["dateModified"]?.asString

        val tags = data["tags"]?.asJsonArray ?: JsonArray()

        val rawType = ItemTypes.annotation
        val collectionKeys = emptySet<String>()
        val parentKey = data["parentItem"].asString

        val dateAddedParsed = dateAdded?.let { iso8601DateFormatV2.parse(it) } ?: Date()
        val dateModifiedParsed = dateModified?.let { iso8601DateFormatV2.parse(it) } ?: Date()

        val isTrash = data["deleted"]?.asBoolean ?: (data["deleted"]?.asInt == 1)
        val tagsParsed = tags.map { tagResponseMapper.fromJson(it.asJsonObject) }
        val relations = JsonObject()
        val inPublications = false
        return ItemResponse(
            version = version,
            tags = tagsParsed,
            parsedDate = parsedDate,
            links = links,
            library = library,
            lastModifiedBy = lastModifiedBy,
            parentKey = parentKey,
            key = key,
            isTrash = isTrash,
            inPublications = inPublications,
            dateModified = dateModifiedParsed,
            dateAdded = dateAddedParsed,
            relations = relations,
            creators = emptyList(),
            createdBy = createdBy,
            collectionKeys = collectionKeys,
            rawType = rawType
        )
    }

}
