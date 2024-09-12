package org.zotero.android.api.pojo.sync

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.helpers.formatter.iso8601DateFormatV2
import org.zotero.android.ktx.rounded
import org.zotero.android.ktx.unmarshalList
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.LinkMode
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SchemaError
import timber.log.Timber
import java.util.Date

data class ItemResponse(
    val rawType: String,
    val key: String,
    val library: LibraryResponse,
    val parentKey: String?,
    val collectionKeys: Set<String>,
    val links: LinksResponse?,
    val parsedDate: String?,
    val isTrash: Boolean,
    val version: Int,
    val dateModified: Date,
    val dateAdded: Date,
    val fields: Map<KeyBaseKeyPair, String>,
    val tags: List<TagResponse>,
    val creators: List<CreatorResponse>,
    val relations: JsonObject,
    val inPublications: Boolean,
    val createdBy: UserResponse?,
    val lastModifiedBy: UserResponse?,
    val rects: List<List<Double>>?,
    val paths: List<List<Double>>?,

    ) {

    var notes: MutableList<String> = mutableListOf()
    var attachments: MutableList<String> = mutableListOf()

    companion object {
        fun parseFields(
            data: JsonObject,
            rawType: String,
            key: String,
            schemaController: SchemaController,
            ignoreUnknownFields: Boolean = false,
            gson: Gson,
            gsonWithRoundedDecimals: Gson,
        ): Triple<Map<KeyBaseKeyPair, String>, List<List<Double>>?, List<List<Double>>?> {
            val excludedKeys = FieldKeys.Item.knownNonFieldKeys
            var fields = mutableMapOf<KeyBaseKeyPair, String>()
            var rects: List<List<Double>>? = null
            var paths: List<List<Double>>? = null

            val schemaFields = schemaController.fields(rawType)
            if (schemaFields == null) {
                throw SchemaError.missingSchemaFields(rawType)
            }

            for (objectS in data.entrySet()) {
                if (excludedKeys.contains(objectS.key)) {
                    continue
                }

                if (!isKnownField(objectS.key, schemaFields, rawType)) {
                    if (ignoreUnknownFields) {
                        continue
                    }
                    throw SchemaError.unknownField(key = key, field = objectS.key)
                }

                var value: String
                if (objectS.value?.isJsonPrimitive == true) {
                    value = objectS.value.asString
                } else {
                    value = "${objectS.value}"
                }

                when (objectS.key) {
                    FieldKeys.Item.Annotation.position -> {
                        val (_rects, _paths) =
                            parsePositionFields(
                                encoded = objectS.value,
                                key = key,
                                fields = fields,
                                gson = gson,
                                gsonWithRoundedDecimals = gsonWithRoundedDecimals,
                            )
                        rects = _rects
                        paths = _paths
                    }

                    FieldKeys.Item.accessDate -> {
                        if (value == "CURRENT_TIMESTAMP") {
                            value = iso8601DateFormatV2.format(Date())
                        }
                        fields[KeyBaseKeyPair(key = objectS.key, baseKey = null)] = value

                    }

                    else -> {
                        fields[KeyBaseKeyPair(key = objectS.key, baseKey = null)] = value

                    }
                }
            }

            validate(
                fields = fields,
                itemType = rawType,
                key = key,
                hasPaths = (paths != null),
                hasRects = (rects != null)
            )
            return Triple(fields, rects, paths)
        }

        private fun validate(
            fields: Map<KeyBaseKeyPair, String>,
            itemType: String,
            key: String,
            hasPaths: Boolean,
            hasRects: Boolean
        ) {
            when (itemType) {
                ItemTypes.annotation -> {
                    val rawType =
                        fields[KeyBaseKeyPair(key = FieldKeys.Item.Annotation.type, baseKey = null)]
                    if (rawType == null) {
                        throw SchemaError.missingField(
                            key = key,
                            field = FieldKeys.Item.Annotation.type,
                            itemType = itemType
                        )
                    }
                    val type: AnnotationType
                    try {
                        type = AnnotationType.valueOf(rawType)
                    } catch (e: Exception) {
                        throw SchemaError.invalidValue(
                            value = rawType,
                            field = FieldKeys.Item.Annotation.type,
                            key = key
                        )
                    }

                    val mandatoryFields = FieldKeys.Item.Annotation.mandatoryApiFields(type)
                    for (field in mandatoryFields) {
                        val value = fields[field]
                        if (value == null) {
                            throw SchemaError.missingField(
                                key = key,
                                field = field.key,
                                itemType = itemType
                            )
                        }


                        when (field.key) {
                            FieldKeys.Item.Annotation.color -> {
                                if (!value.startsWith("#")) {
                                    throw SchemaError.invalidValue(
                                        value = value,
                                        field = field.key,
                                        key = key
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
                ItemTypes.attachment -> {
                    val rawLinkMode = fields[KeyBaseKeyPair(
                        key = FieldKeys.Item.Attachment.linkMode,
                        baseKey = null
                    )]
                    if (rawLinkMode == null) {
                        throw SchemaError.missingField(
                            key = key,
                            field = FieldKeys.Item.Attachment.linkMode,
                            itemType = itemType
                        )
                    }
                    try {
                        LinkMode.from(rawLinkMode)
                    } catch (e: Exception) {
                        Timber.e(e)

                        throw SchemaError.invalidValue(
                            value = rawLinkMode,
                            field = FieldKeys.Item.Attachment.linkMode,
                            key = key
                        )
                    }
                }
                else -> return
            }
        }

        private fun isKnownField(field: String, schema: List<FieldSchema>, itemType: String): Boolean {
            if (field == FieldKeys.Item.note || schema.find { it.field == field } != null) {
                return true
            }

            return when (itemType) {
                ItemTypes.annotation ->
                    FieldKeys.Item.Annotation.knownKeys.contains(field)
                ItemTypes.attachment ->
                    FieldKeys.Item.Attachment.knownKeys.contains(field)
                else -> false
            }
        }

        private fun parsePositionFields(
            encoded: JsonElement?,
            key: String,
            fields: MutableMap<KeyBaseKeyPair, String>,
            gson: Gson,
            gsonWithRoundedDecimals: Gson
        ): Pair<List<List<Double>>?, List<List<Double>>?> {
            if (encoded == null) {
                throw SchemaError.invalidValue(value  = encoded?.asString ?: "", field =  FieldKeys.Item.Annotation.position, key =  key)
            }
            val json = gson.fromJson(encoded.asString, JsonObject::class.java)

            var rects: List<List<Double>>? = null
            var paths: List<List<Double>>? = null

            for (objectS in json.entrySet()) {
                when (objectS.key) {
                    FieldKeys.Item.Annotation.Position.paths -> {
                        val parsedPaths = objectS.value?.unmarshalList<List<Double>>(gson)
                        if (parsedPaths != null && (parsedPaths.isNotEmpty() && parsedPaths.firstOrNull { it.size % 2 != 0 } == null)) {
                            paths = parsedPaths
                        }
                        continue
                    }

                    FieldKeys.Item.Annotation.Position.rects -> {
                        val parsedRects = objectS.value?.unmarshalList<List<Double>>(gson)
                        if (parsedRects != null && (parsedRects.isNotEmpty() && parsedRects.firstOrNull { it.size != 4 } == null)) {
                            rects = parsedRects
                        }
                        continue
                    }
                }

                val asStr = objectS.value?.asString
                val asInt = asStr?.toIntOrNull()
                val asDouble = asStr?.toDoubleOrNull()
                val asBoolean = asStr?.toBooleanStrictOrNull()
                val value = if (asInt != null) {
                    asInt.toString()
                } else if (asDouble != null) {
                    asDouble.rounded(3).toString()
                } else if (asBoolean != null) {
                    asBoolean.toString()
                } else {
                    try {
                        gsonWithRoundedDecimals.toJson(objectS)
                    } catch (e: Exception) {
                        Timber.e(e)
                        asStr ?: "null"
                    }

                }

                fields[KeyBaseKeyPair(key = objectS.key, baseKey = FieldKeys.Item.Annotation.position)] = value
            }

            return rects to paths
        }
    }

    fun copy(
        libraryId: LibraryIdentifier,
        collectionKeys: Set<String>,
        tags: List<TagResponse>
    ): ItemResponse {
        return ItemResponse(
            rawType = this.rawType,
            key = this.key,
            library = LibraryResponse.init(libraryId = libraryId),
            parentKey = this.parentKey,
            collectionKeys = collectionKeys,
            links = this.links,
            parsedDate = this.parsedDate,
            isTrash = this.isTrash,
            version = this.version,
            dateModified = this.dateModified,
            dateAdded = this.dateAdded,
            fields = this.fields,
            tags = tags,
            creators = this.creators,
            relations = this.relations,
            createdBy = this.createdBy,
            lastModifiedBy = this.lastModifiedBy,
            rects = this.rects,
            paths = this.paths,
            inPublications = false,
        )
    }

    val copyWithAutomaticTags: ItemResponse
        get() {
            return ItemResponse(
                rawType = this.rawType,
                key = this.key,
                library = this.library,
                parentKey = this.parentKey,
                collectionKeys = this.collectionKeys,
                links = this.links,
                parsedDate = this.parsedDate,
                isTrash = this.isTrash,
                version = this.version,
                dateModified = this.dateModified,
                dateAdded = this.dateAdded,
                fields = this.fields,
                tags = this.tags.map { it.automaticCopy },
                creators = this.creators,
                relations = this.relations,
                createdBy = this.createdBy,
                lastModifiedBy = this.lastModifiedBy,
                rects = this.rects,
                paths = this.paths,
                inPublications = false,
            )
    }
}


data class KeyBaseKeyPair(
    val key: String,
    val baseKey: String?
)


