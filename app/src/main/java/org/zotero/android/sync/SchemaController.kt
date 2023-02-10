package org.zotero.android.sync

import org.zotero.android.api.pojo.sync.CreatorSchema
import org.zotero.android.api.pojo.sync.FieldSchema
import org.zotero.android.api.pojo.sync.ItemSchema
import org.zotero.android.api.pojo.sync.SchemaLocale
import org.zotero.android.api.mappers.SchemaResponseMapper
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.files.FileStore
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchemaController @Inject constructor(
    private val fileStore: FileStore,
    private val schemaResponseMapper: SchemaResponseMapper
) {

    private var itemSchemas: Map<String, ItemSchema> = emptyMap()
    private var locales: Map<String, SchemaLocale> = emptyMap()
    private var version: Int = 0

    init {
        loadBundledData()
    }

    private fun loadBundledData() {
        val json = fileStore.getBundledSchema() ?: return
        val schema = schemaResponseMapper.fromJson(json)
        itemSchemas = schema.itemSchemas
        locales = schema.locales
        version = schema.version
    }

    val itemTypes: List<String>
        get() {
            return itemSchemas.keys.toList()
        }

    fun fields(type: String): List<FieldSchema>? {
        return itemSchemas[type]?.fields
    }

    fun titleKey(type: String): String? {
        return fields(type = type)?.first {
            it.field == FieldKeys.Item.title ||
                    it.baseField == FieldKeys.Item.title
        }?.field
    }

    fun baseKey(type: String, field: String): String? {
        return fields(type = type)?.firstOrNull { it.field == field }?.baseField
    }

    fun creators(type: String): List<CreatorSchema>? {
        return itemSchemas[type]?.creatorTypes
    }

    fun creatorIsPrimary(creatorType: String, itemType: String): Boolean {
        return creators(itemType)?.first { it.creatorType == creatorType }?.primary ?: false
    }

    fun locale(localeId: String): SchemaLocale? {
        if (locales[localeId] != null) {
            return locales[localeId]
        }

        val languagePart = localeId.split("_").firstOrNull() ?: localeId
        val locale = locales.entries.firstOrNull { it.key.contains(languagePart) }?.value
        if (locale != null) {
            return locale
        }
        return locales["en_US"]
    }

    private val currentLocale: SchemaLocale?
        get() {
            val localeId = Locale.getDefault().toString()
            return locale(localeId)
        }

    fun localizedItemType(itemType: String): String? {
        return currentLocale?.itemTypes?.get(itemType)
    }

    fun localizedField(field: String): String? {
        return currentLocale?.fields?.get(field)
    }

    fun localizedCreator(creator: String): String? {
        return currentLocale?.creatorTypes?.get(creator)
    }
}