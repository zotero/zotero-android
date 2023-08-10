package org.zotero.android.api.mappers

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.zotero.android.api.ForGsonWithRoundedDecimals
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.api.pojo.sync.LibraryResponse
import org.zotero.android.api.pojo.sync.LinksResponse
import org.zotero.android.api.pojo.sync.UserResponse
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.helpers.formatter.iso8601DateFormatV2
import org.zotero.android.ktx.convertFromBooleanOrIntToBoolean
import org.zotero.android.ktx.unmarshalList
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.LinkMode
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SchemaError
import java.util.Date
import javax.inject.Inject

class ItemResponseMapper @Inject constructor(
    private val creatorResponseMapper: CreatorResponseMapper,
    private val libraryResponseMapper: LibraryResponseMapper,
    private val linksResponseMapper: LinksResponseMapper,
    private val tagResponseMapper: TagResponseMapper,
    private val userResponseMapper: UserResponseMapper,
    private val gson: Gson,
    @ForGsonWithRoundedDecimals
    private val gsonWithRoundedDecimals: Gson
) {
    fun fromJson(json: JsonObject, schemaController: SchemaController): ItemResponse {
        val data = json["data"].asJsonObject
        val key: String = json["key"].asString
        val itemType = data["itemType"].asString

        if (!schemaController.itemTypes.contains(itemType)) {
            throw SchemaError.invalidValue(value = itemType, field = "itemType", key = key)
        }

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
                    data = data,
                    schemaController = schemaController
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
                    rawType = itemType,
                    schemaController = schemaController,
                    gson = gson,
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
        data: JsonObject,
        schemaController: SchemaController,
        gson: Gson,
    ): ItemResponse {
        val dateAdded = data["dateAdded"]?.asString
        val dateModified = data["dateModified"]?.asString
        val tags = data["tags"]?.asJsonArray ?: JsonArray()
        val creators = data["creators"]?.asJsonArray ?: JsonArray()

        val collectionKeys =
            data["collections"]?.unmarshalList<String>(gson)?.toSet() ?: emptySet<String>()
        val parentKey = data["parentItem"]?.asString
        val dateAddedParsed = dateAdded?.let { iso8601DateFormatV2.parse(it) } ?: Date()
        val dateModifiedParsed = dateModified?.let { iso8601DateFormatV2.parse(it) } ?: Date()
        val isTrash = data["deleted"].convertFromBooleanOrIntToBoolean()
        val tagsParsed = tags.map { tagResponseMapper.fromJson(it.asJsonObject) }
        val creatorsParsed = creators.map { creatorResponseMapper.fromJson(it.asJsonObject) }
        val relations = data["relations"]?.asJsonObject ?: JsonObject()
        val inPublications = data["inPublications"].convertFromBooleanOrIntToBoolean()

        val fields = ItemResponse.parseFields(
            data = data,
            rawType = rawType,
            key = key,
            schemaController = schemaController,
            gson = gson,
            gsonWithRoundedDecimals = gsonWithRoundedDecimals,
        ).first

        if (rawType == ItemTypes.attachment) {
            val linkMode = fields[KeyBaseKeyPair(
                key = FieldKeys.Item.Attachment.linkMode,
                baseKey = null
            )]?.let { LinkMode.from(it) }
            if (linkMode != null &&
                linkMode == LinkMode.embeddedImage && parentKey == null
            ) {
                throw SchemaError.embeddedImageMissingParent(
                    key = key, libraryId = library.libraryId ?: LibraryIdentifier.custom(
                        RCustomLibraryType.myLibrary
                    )
                )
            }
        }

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
            version = version,
            paths = null,
            rects = null,
            fields = fields
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
        data: JsonObject,
        schemaController: SchemaController
    ): ItemResponse {
        val dateAdded = data["dateAdded"]?.asString
        val dateModified = data["dateModified"]?.asString

        val tags = data["tags"]?.asJsonArray ?: JsonArray()

        val rawType = ItemTypes.annotation
        val collectionKeys = emptySet<String>()
        val parentKey = data["parentItem"].asString

        val dateAddedParsed = dateAdded?.let { iso8601DateFormatV2.parse(it) } ?: Date()
        val dateModifiedParsed = dateModified?.let { iso8601DateFormatV2.parse(it) } ?: Date()

        val isTrash = data["deleted"].convertFromBooleanOrIntToBoolean()
        val tagsParsed = tags.map { tagResponseMapper.fromJson(it.asJsonObject) }
        val relations = JsonObject()
        val inPublications = false

        val (fields, rects, paths) = ItemResponse.parseFields(
            data = data,
            rawType = ItemTypes.annotation,
            key = key,
            schemaController = schemaController,
            gson = gson,
            gsonWithRoundedDecimals = gsonWithRoundedDecimals,
        )

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
            rawType = rawType,
            fields = fields,
            rects = rects,
            paths = paths
        )
    }

}
