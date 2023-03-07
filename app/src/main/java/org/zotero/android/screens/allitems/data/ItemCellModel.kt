package org.zotero.android.screens.allitems.data

import androidx.compose.ui.graphics.Color
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.isTrash
import org.zotero.android.database.requests.items
import org.zotero.android.database.requests.key
import org.zotero.android.uicomponents.attachmentprogress.State

data class ItemCellModel(
    val key: String,
    val typeIconName: Int,
    val typeName: String,
    val title: String,
    val subtitle: String,
    val hasNote: Boolean,
    var accessory: Accessory?,
    val tagColors: List<Color>,
) {

    sealed class Accessory {
        data class attachment(val state: State) : Accessory()
        object doi : Accessory()
        object url : Accessory()
    }

    companion object {
        fun init(item: RItem, typeName: String, accessory: Accessory?): ItemCellModel {
            val contentType = if (item.rawType == ItemTypes.attachment) item.fields.where().key(
                FieldKeys.Item.Attachment.contentType
            ).findFirst()?.value else null

            return ItemCellModel(
                key = item.key,
                typeIconName = ItemTypes.iconName(
                    rawType = item.rawType,
                    contentType = contentType
                ),
                typeName = typeName,
                title = item.displayTitle,
                subtitle = subtitle(item = item),
                hasNote = hasNote(item = item),
                accessory = accessory,
                tagColors = tagColors(item = item)
            )
        }

        fun tagColors(item: RItem): List<Color> {
            return listOf()//TODO implement
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

    }
    fun updateAccessory(cellAccessory: Accessory?) {
        this.accessory = cellAccessory
    }
}