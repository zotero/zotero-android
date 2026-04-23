package org.zotero.android.database.migration.main.solutions

import io.realm.DynamicRealm
import io.realm.DynamicRealmObject
import io.realm.FieldAttribute
import org.zotero.android.database.objects.AllItemsDbRow
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.key
import org.zotero.android.sync.LinkMode

class MigrateAllItemsDbRowTypeIconNameTypeChange(private val dynamicRealm: DynamicRealm) {

    fun migrate() {
        val realmSchema = dynamicRealm.schema

        val allItemsDbRowSchema = realmSchema.get(AllItemsDbRow::class.java.simpleName)

        allItemsDbRowSchema?.run {
            removeField("typeIconName")
            addField("typeIconName", String::class.java, FieldAttribute.REQUIRED)
            transform {
                it.set("typeIconName", "")
            }
        }
        val allItems = dynamicRealm.where(RItem::class.java.simpleName).findAll()
        for (item in allItems) {
            val dbRow = item.getObject("allItemsDbRow")
            if (dbRow == null) {
                continue
            }

            dbRow.setString(
                "typeIconName", typeIconName(item)
            )
        }

    }

    private fun typeIconName(item: DynamicRealmObject): String {
        val rawType = item.getString("rawType")
        val fields = item.getList("fields")

        var data: ItemTypes.AttachmentData? = null
        if (rawType == ItemTypes.Companion.attachment) {
            val contentType = fields.where().key(FieldKeys.Item.Attachment.contentType)
                .findFirst()?.getString("value")
            if (contentType != null) {
                val linkMode =
                    fields.where().key(FieldKeys.Item.Attachment.linkMode).findFirst()
                        ?.getString("value")
                        ?.let {
                            LinkMode.from(it)
                        }
                if (linkMode != null) {
                    data = ItemTypes.AttachmentData(
                        contentType = contentType,
                        linked = linkMode == LinkMode.linkedFile
                    )
                }
            }
        }
        return ItemTypes.iconName(rawType, attachmentData = data)
    }
}