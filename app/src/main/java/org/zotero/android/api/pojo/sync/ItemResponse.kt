package org.zotero.android.api.pojo.sync

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.zotero.android.architecture.database.objects.AnnotationType
import org.zotero.android.architecture.database.objects.FieldKeys
import org.zotero.android.architecture.database.objects.ItemTypes
import org.zotero.android.formatter.iso8601DateFormat
import org.zotero.android.ktx.rounded
import org.zotero.android.ktx.unmarshalList
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
            data: JsonObject, rawType: String, key: String, schemaController: SchemaController,
            ignoreUnknownFields: Boolean = false) : Triple<Map<KeyBaseKeyPair, String>, List<List<Double>>?, List<List<Double>>?> {
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
                            parsePositionFields(objectS.value, key = key, fields = fields)
                        rects = _rects
                        paths = _paths
                    }

                    FieldKeys.Item.accessDate -> {
                        if (value == "CURRENT_TIMESTAMP") {
                            value = iso8601DateFormat.format(Date())
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
                    when (type) {
                        AnnotationType.note, AnnotationType.image, AnnotationType.highlight -> {
                            if (!hasRects) {
                                throw SchemaError.missingField(
                                    key = key,
                                    field = FieldKeys.Item.Annotation.Position.rects,
                                    itemType = itemType
                                )
                            }
                        }

                        AnnotationType.ink -> {
                            if (!hasPaths) {
                                throw SchemaError.missingField(
                                    key = key,
                                    field = FieldKeys.Item.Annotation.Position.paths,
                                    itemType = itemType
                                )
                            }
                        }

                    }

                    val mandatoryFields = FieldKeys.Item.Annotation.fields(type)
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


                            FieldKeys.Item.Annotation.sortIndex -> {
                                val parts = value.split("|")
                                if (parts.size != 3 || parts[0].length != 5 || parts[1].length != 6 || parts[2].length != 5) {
                                    throw SchemaError.invalidValue(
                                        value = value,
                                        field = field.key, key = key
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

        private fun parsePositionFields(encoded: JsonElement?, key: String, fields: MutableMap<KeyBaseKeyPair, String>) : Pair<List<List<Double>>?, List<List<Double>>?>  {
            if (encoded == null) {
                throw SchemaError.invalidValue(value  = encoded?.asString ?: "", field =  FieldKeys.Item.Annotation.position, key =  key)
            }
            val json = Gson().fromJson(encoded.asString, JsonObject::class.java)

            var rects: List<List<Double>>? = null
            var paths: List<List<Double>>? = null

            for (objectS in json.entrySet()) {
                when (objectS.key) {
                    FieldKeys.Item.Annotation.Position.pageIndex -> {
                        if (objectS.value?.asInt == null) {
                            throw SchemaError.invalidValue(
                                value = "${objectS.value}",
                                field = FieldKeys.Item.Annotation.Position.pageIndex,
                                key = key
                            )
                        }
                    }


                    FieldKeys.Item.Annotation.Position.lineWidth -> {
                        if (objectS.value?.asDouble == null) {
                            throw SchemaError.invalidValue(
                                value = "${objectS.value}",
                                field = FieldKeys.Item.Annotation.Position.lineWidth,
                                key = key
                            )
                        }
                    }


                    FieldKeys.Item.Annotation.Position.paths -> {
                        val parsedPaths = objectS.value?.unmarshalList<List<Double>>()
                        if (parsedPaths == null || (parsedPaths.isEmpty() || parsedPaths.firstOrNull { it.size % 2 != 0 } != null)) {
                            throw SchemaError.invalidValue(
                                value = "${objectS.value}",
                                field = FieldKeys.Item.Annotation.Position.paths,
                                key = key
                            )
                        }

                        paths = parsedPaths
                        continue
                    }

                FieldKeys.Item.Annotation.Position.rects -> {
                    val parsedRects = objectS.value?.unmarshalList<List<Double>>()
                    if (parsedRects == null || (parsedRects.isEmpty() || parsedRects.firstOrNull { it.size != 4 } != null)) {
                        throw SchemaError.invalidValue(
                            value = "${objectS.value}",
                            field = FieldKeys.Item.Annotation.Position.rects,
                            key = key
                        )
                    }
                    rects = parsedRects
                    continue
                }
            }

                val asStr = objectS.value?.asString
                val asInt = asStr?.toIntOrNull()
                val asDouble = asStr?.toDoubleOrNull()
                val value = if (asInt != null) {
                    asInt.toString()
                } else if(asDouble != null) {
                    asDouble.rounded(3).toString()
                } else{
                    asStr ?: "null"
                }

                fields[KeyBaseKeyPair(key = objectS.key, baseKey = FieldKeys.Item.Annotation.position)] = value
            }

            return rects to paths
        }


    }
}


data class KeyBaseKeyPair(
    val key: String,
    val baseKey: String?
)


