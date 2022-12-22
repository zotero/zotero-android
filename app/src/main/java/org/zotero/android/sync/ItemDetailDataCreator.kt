package org.zotero.android.sync

import org.zotero.android.architecture.database.objects.Attachment
import org.zotero.android.architecture.database.objects.FieldKeys
import org.zotero.android.architecture.database.objects.ItemTypes
import org.zotero.android.architecture.database.objects.ObjectSyncState
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.requests.items
import org.zotero.android.dashboard.data.ItemDetailCreator
import org.zotero.android.dashboard.data.ItemDetailData
import org.zotero.android.dashboard.data.ItemDetailError
import org.zotero.android.dashboard.data.ItemDetailField
import org.zotero.android.files.FileStore
import org.zotero.android.formatter.dateAndTimeFormat
import org.zotero.android.formatter.iso8601DateFormatV2
import org.zotero.android.formatter.sqlFormat
import timber.log.Timber
import java.util.Date
import java.util.UUID

object ItemDetailDataCreator {

    sealed class Kind {
        data class new(val itemType: String, val child: Attachment?): Kind()
        data class existing(val item: RItem, val ignoreChildren: Boolean): Kind()
    }

    fun createData(type: Kind, schemaController: SchemaController, fileStorage: FileStore, urlDetector: UrlDetector, doiDetector: (String) -> Boolean): ItemDetailCreateDataResult{
        when (type) {
            is Kind.new ->
            return creationData(itemType = type.itemType, child = type.child, schemaController = schemaController,
                urlDetector = urlDetector, doiDetector = doiDetector)
            is Kind.existing ->
            return itemData(item = type.item, ignoreChildren = type.ignoreChildren, schemaController = schemaController,
                fileStorage = fileStorage, urlDetector = urlDetector, doiDetector = doiDetector)
        }
    }

    private fun itemData(item: RItem, ignoreChildren: Boolean,schemaController: SchemaController,
                         fileStorage: FileStore, urlDetector: UrlDetector, doiDetector: (String) -> Boolean)
    : ItemDetailCreateDataResult {
        val localizedType = schemaController.localizedItemType(item.rawType)
        if (localizedType == null) {
            throw ItemDetailError.typeNotSupported(item.rawType)

        }

        var abstract: String? = null
        var values = mutableMapOf<String, String>()

        item.fields.forEach { field ->
            when (field.key) {
                FieldKeys.Item.abstractN ->
                    abstract = field.value
                else ->
                    values[field.key] = field.value
            }
        }

        val (fieldIds, fields, _) = fieldData(item.rawType, schemaController = schemaController,
            urlDetector = urlDetector, doiDetector = doiDetector, getExistingData = { key, _ ->
                return@fieldData null to values[key]
        })

        var creatorIds = mutableListOf<UUID>()
        var creators = mutableMapOf<UUID, ItemDetailCreator>()
        for (creator in item.creators.sort("orderId")) {
            val localizedType = schemaController.localizedCreator(creator.rawType)
            if (localizedType == null) {
                continue
            }
            val creator = ItemDetailCreator.init(firstName = creator.firstName,
            lastName = creator.lastName,
            fullName = creator.name,
            type = creator.rawType,
            primary = schemaController.creatorIsPrimary(creator.rawType, itemType = item.rawType),
            localizedType = localizedType)
            creatorIds.add(creator.id)
            creators[creator.id] = creator
        }

        val notes:MutableList<Note>
        if (ignoreChildren) {
            notes = mutableListOf()
        } else {
            notes = item.children!!.where()
                .items(type = ItemTypes.note, notSyncState = ObjectSyncState.dirty, trash = false)
                .sort("displayTitle")
                .findAll().mapNotNull { Note.init(it) }.toMutableList()
        }
        val attachments: List<Attachment>
        if(ignoreChildren) {
            attachments = mutableListOf()
        } else if (item.rawType == ItemTypes.attachment) {
            val attachment = AttachmentCreator.attachment(item, fileStorage = fileStorage, urlDetector = urlDetector, isForceRemote = false)
            attachments = attachment?.let { listOf(it) } ?: emptyList()
        } else {
            val mappedAttachments = item.children!!.where().items(type = ItemTypes.attachment, notSyncState = ObjectSyncState.dirty, trash=  false)
            .sort("displayTitle")
            .findAll().mapNotNull{ item ->
                AttachmentCreator.attachment(item = item,
                    fileStorage = fileStorage,
                    isForceRemote = false,
                    urlDetector = urlDetector)
            }
            attachments = mappedAttachments
        }

        val tags = item.tags!!.sort("tag.name").map{Tag(it)}
        val data = ItemDetailData(title = item.baseTitle,
        type =item.rawType,
        isAttachment = (item.rawType == ItemTypes.attachment),
        localizedType = localizedType,
        creators = creators,
        creatorIds = creatorIds,
        fields = fields,
        fieldIds = fieldIds,
        abstract = abstract,
        dateModified = item.dateModified,
        dateAdded = item.dateAdded)
        return ItemDetailCreateDataResult(data, attachments, notes, tags)
    }

    private fun creationData(itemType: String, child: Attachment?, schemaController: SchemaController, urlDetector: UrlDetector,
    doiDetector: (String) -> Boolean): ItemDetailCreateDataResult {
        val localizedType = schemaController.localizedItemType(itemType)
        if (localizedType == null) {
            Timber.e("ItemDetailDataCreator: schema not initialized - can't create localized type")
            throw ItemDetailError.cantCreateData
        }

        val (fieldIds, fields, hasAbstract) = fieldData(itemType, schemaController = schemaController, urlDetector = urlDetector, doiDetector = doiDetector)
            val date = Date()
            val attachments: List<Attachment> = child?.let { listOf(it)} ?: emptyList()
            val data = ItemDetailData(title = "",
            type = itemType,
            isAttachment = (itemType == ItemTypes.attachment),
            localizedType = localizedType,
            creators = mapOf(),
            creatorIds = emptyList(),
            fields = fields,
            fieldIds = fieldIds,
            abstract = if (hasAbstract) "" else null,
            dateModified = date,
            dateAdded = date)

            return ItemDetailCreateDataResult(data, attachments, emptyList(), emptyList())
        }

    fun fieldData(itemType: String, schemaController: SchemaController, urlDetector: UrlDetector, doiDetector: (String) -> Boolean,
        getExistingData: ((String, String?) -> Pair<String?, String?>)? = null): Triple<List<String>, Map<String, ItemDetailField>, Boolean> {
        val fieldSchemas = schemaController.fields(itemType)?.toMutableList()
        if (fieldSchemas == null) {
            throw ItemDetailError.typeNotSupported(itemType)
        }
        var fieldKeys = fieldSchemas.map{ it.field }.toMutableList()
        val abstractIndex = fieldKeys.indexOf(FieldKeys.Item.abstractN)

        if (abstractIndex != -1) {
            fieldKeys.removeAt(abstractIndex)
            fieldSchemas.removeAt(abstractIndex)
        }
        val key = schemaController.titleKey(itemType)
        val index = fieldKeys.indexOf(key)
        if (key != null && index != -1) {
            fieldKeys.removeAt(index)
            fieldSchemas.removeAt(index)
        }

        val fields = mutableMapOf<String, ItemDetailField>()
        for ((offset, key) in fieldKeys.withIndex()) {
            val baseField = fieldSchemas[offset].baseField
            val (existingName, existingValue) = if (getExistingData != null) {
                getExistingData(key, baseField)
            } else {
                Pair(null, null)
            }

            val name = existingName ?: schemaController.localizedField(key) ?: ""
            val value = existingValue ?: ""
            val isTappable = ItemDetailDataCreator.isTappable(key = key, value = value, urlDetector = urlDetector, doiDetector = doiDetector)
            var additionalInfo: Map<ItemDetailField.AdditionalInfoKey, String>? = null

            //TODO Parse dateOrder

            if (key == FieldKeys.Item.accessDate) {
                if (value.isNotEmpty()) {
                    val date = iso8601DateFormatV2.parse(value)!!
                    additionalInfo = mapOf(ItemDetailField.AdditionalInfoKey.formattedDate to dateAndTimeFormat.format(date),
                        ItemDetailField.AdditionalInfoKey.formattedEditDate to sqlFormat.format(date))
                }
            }


            fields[key] = ItemDetailField(key = key,
                baseField = baseField,
                name = name,
                value = value,
                isTitle = false,
            isTappable = isTappable,
            additionalInfo = additionalInfo)
        }

        return Triple(fieldKeys, fields, (abstractIndex != -1))
    }

    fun isTappable(key: String, value: String, urlDetector: UrlDetector, doiDetector: (String) -> Boolean): Boolean {
        when(key) {
            FieldKeys.Item.doi ->
            return doiDetector(value)
            FieldKeys.Item.Attachment.url ->
            return urlDetector.isUrl(value)
            else ->
            return false
        }
    }

    fun filteredFieldKeys(fieldKeys: List<String>, fields: Map<String, ItemDetailField>): List<String> {
        var newFieldKeys = mutableListOf<String>()
        fieldKeys.forEach { key ->
            if (!(fields[key]?.value ?: "").isEmpty()) {
                newFieldKeys.add(key)
            }
        }
        return newFieldKeys
    }


}

data class ItemDetailCreateDataResult(val itemData: ItemDetailData, val attachments: List<Attachment>, val notes: List<Note>, val tags: List<Tag>)