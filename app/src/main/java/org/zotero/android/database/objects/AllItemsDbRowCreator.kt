package org.zotero.android.database.objects

import io.realm.Realm
import org.zotero.android.database.requests.isTrash
import org.zotero.android.database.requests.items
import org.zotero.android.database.requests.key

class AllItemsDbRowCreator {

    companion object {

        fun createOrUpdate(item: RItem, database: Realm): AllItemsDbRow? {
            val dbRow = item.allItemsDbRow ?: database.createEmbeddedObject(AllItemsDbRow::class.java, item, "allItemsDbRow")

            val contentType = if (item.rawType == ItemTypes.attachment) item.fields.where().key(
                FieldKeys.Item.Attachment.contentType
            ).findFirst()?.value else null

            dbRow.typeIconName = ItemTypes.iconName(
                rawType = item.rawType,
                contentType = contentType
            )
            dbRow.title = item.displayTitle
            dbRow.subtitle = subtitle(item = item)
            val hasNote = hasNote(item = item)
            dbRow.hasNote = hasNote
            return dbRow
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