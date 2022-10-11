package org.zotero.android.api.pojo.sync

data class SchemaResponse(
    val version: Int,
    val itemSchemas: Map<String, ItemSchema>,
    val locales: Map<String, SchemaLocale>
)

data class ItemSchema(
    val itemType: String,
    val fields: List<FieldSchema>,
    val creatorTypes: List<CreatorSchema>
)

data class FieldSchema(
    val field: String,
    val baseField: String?
)

data class CreatorSchema(
    val creatorType: String,
    val primary: Boolean
)

data class SchemaLocale(
    val itemTypes: Map<String, String>,
    val fields: Map<String, String>,
    val creatorTypes: Map<String, String>
)
