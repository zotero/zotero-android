package org.zotero.android.database.objects

import io.realm.Realm
import org.zotero.android.database.requests.isTrash
import org.zotero.android.database.requests.items
import org.zotero.android.database.requests.key
import org.zotero.android.sync.LinkMode

class AllItemsDbRowCreator {

    companion object {

        fun createOrUpdate(item: RItem, database: Realm): AllItemsDbRow? {
            val dbRow = item.allItemsDbRow ?: database.createEmbeddedObject(AllItemsDbRow::class.java, item, "allItemsDbRow")

            val typeIconName = typeIconName(item)
            dbRow.typeIconName = typeIconName
            dbRow.title = item.displayTitle
            dbRow.subtitle = subtitle(item = item)
            val hasNote = hasNote(item = item)
            dbRow.hasNote = hasNote
            return dbRow
        }

        private fun typeIconName(item: RItem): String {
            var data: ItemTypes.AttachmentData? = null
            if (item.rawType == ItemTypes.attachment) {
                val contentType = item.fields.where().key(FieldKeys.Item.Attachment.contentType)
                    .findFirst()?.value
                if (contentType != null) {
                    val linkMode =
                        item.fields.where().key(FieldKeys.Item.Attachment.linkMode).findFirst()
                            ?.let {
                                LinkMode.from(it.value)
                            }
                    if (linkMode != null) {
                        data = ItemTypes.AttachmentData(
                            contentType = contentType,
                            linked = linkMode == LinkMode.linkedFile
                        )
                    }
                }
            }
            return ItemTypes.iconName(item.rawType, attachmentData = data)
        }

        private fun subtitle(item: RItem): String {
            if (item.creatorSummary == null && item.parsedYear == 0) {
                return ""
            }
            var result = item.creatorSummary ?: ""
            if (!result.isEmpty()) {
                result += " "
            }
            if (item.parsedYear > 0) {
                result += "(${item.parsedYear})"
            }
            return result
        }

        private fun hasNote(item: RItem): Boolean {
            return item.children!!
                .where()
                .items(type = ItemTypes.note, notSyncState = ObjectSyncState.dirty)
                .and()
                .isTrash(false)
                .findAll()
                .isNotEmpty()
        }
    }


}